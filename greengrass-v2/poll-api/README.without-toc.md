# Local API polling example for Greengrass V2

<!-- toc -->

Many applications have local APIs that need to be polled for updates. This reference architecture implements the
following components to show a repeatable pattern for working with these APIs and safely relaying the API data (filtered
or not) to AWS:

- com.greengrass.FakeApi - This is a mock API that returns a fixed set of data.
- com.greengrass.PollApi - This is a function that polls the API and writes the data to stream manager.
- com.greengrass.FilterApiStream - This is a function that reads data from stream manager and filters it (removes data)
  in some way.
- com.greengrass.ProcessApiStream - This is a function that reads data from stream manager and processes it (checks
  data) in some way.
- com.greengrass.ExportStreamToS3 - This is a function that reads data from a stream and uses Stream Manager to export
  it to S3.
- com.greengrass.CleanUpExportFiles - This is a function that monitors the temporary files that Stream Manager uses when
  exporting data to S3 and cleans them up when the export is complete. It also reports if any exports fail and will not
  delete files that contain data that could not be exported.

## How can I get started quickly?

If you already have a Greengrass core running, you can simply copy the artifacts and recipes to your core and merge them with your configuration.

If you'd like to see values exported to S3 with a simple polling setup you can do the following:

- Copy `recipe/com.greengrass.ExportStreamToS3-1.0.0.json.template` to `recipe/com.greengrass.ExportStreamToS3-1.0.0.json`
- Set the `BucketName` value in `recipe/com.greengrass.ExportStreamToS3-1.0.0.json` to the name of your S3 bucket
- Create the bucket if it does not exist already
- Make sure your Greengrass core's permissions allow it to access the bucket
- Run this command:

```
./greengrass/v2/bin/greengrass-cli deployment create \
  --recipeDir recipes \
  --artifactDir artifacts \
  --merge "com.greengrass.FakeApi=1.0.0" \
  --merge "com.greengrass.PollApi=1.0.0" \
  --merge "com.greengrass.ExportStreamToS3=1.0.0" \
  --merge "com.greengrass.CleanUpExportFiles=1.0.0" \
  --merge "com.greengrass.GGUtils=1.0.0" \
  --merge "aws.greengrass.LocalDebugConsole==2.2.3"
```

After the deployment has completed you should start seeing objects show up in your S3 bucket in a few minutes.

## Considerations

### Consideration 1 for exporting data with Stream Manager: Use timestamps instead of sequence numbers in production

In a production environment it is not recommended to use the sequence number from the stream as a key for the S3 object.
Stream Manager will overwrite existing objects with the same key. In a production system it is better to use a key that
will be guaranteed to be unique even when the system is restarted. When a system is restarted the sequence number can be
reset to 0. Using a timestamp as a key is a good way to do this.

### Consideration 2: Make sure all downstream functions are configured to keep up with the data rate of the upstream functions

Make sure that the combination of batch size (if used) and polling interval is large enough to keep up with the data
rate of the upstream components. If the downstream components set these values too low they will fall behind the
upstream component and eventually lose data.

Upstream function: polling interval * records per invocation = number of records added per second Downstream function:
polling interval / batch size = number of records removed per second

Number of records removed per second must always be larger than number of records added per second!

## Components in depth

### FakeApi

#### Description

Fake API is a small Python based Flask application. It simply returns random data (specifically data with a gaussian
distribution with a mu, or mean, of 100, and a sigma, or standard deviation of 5).

This function does not take any parameters but will honor the Flask environment variables if they are set.

In the default configuration it has two endpoints:

- http://localhost:5000/ - This simply returns "Server is running" so you can validate the application is running.
- http://localhost:5000/data - This returns a JSON payload that looks like this:

NOTE: Since this application is only used for testing it does not use HTTPS. In production deployments you would want to
use HTTPS unless the API is only listening on localhost.

NOTE: The JSON example below has been shortened. The real JSON payload would be much longer.

```json
{
  "device_data": {
    "descriptions": [
      "timestamp",
      "name",
      "text_value",
      "numeric_value",
      "source"
    ],
    "points": {
      "device_1": [
        [
          1637941481810,
          "datum",
          "4.343775279510528",
          4.343775279510528,
          0
        ]
      ]
    }
  }
}
```

#### Configuration

Fake API has no configuration in the recipe file but there are two configuration values in `app.py` that can be changed
to generate more or less data:

- number_of_devices - This is the number of devices to generate. The default value is: `10`.
- number_of_values_per_second - This is the number of values to generate per second per device. The default value
  is: `2`.

### PollApi

#### Description

Poll API is a Python component that requests data via HTTP or HTTPS using the Python requests library. It then writes
the data that it receives to a stream.

Poll API utilizes many of the convenience functions in GGUtils to keep the code as concise as possible. A description of
these convenience functions is at the bottom of this document in the GGUtils section.

#### Configuration

Poll API has the following configuration parameters:

- Endpoint - The URL of the endpoint to poll for data. The default value is: `http://localhost:5000/data` so you can use
  this function to poll the Fake API.
- PollIntervalSecs - The number of seconds to wait between polling the endpoint. The default value is: `2` seconds.
- StreamName - The name of the stream where the data from successful requests goes. The default value
  is: `RawApiDataStream`.

### FilterApiStream

#### Description

Filter API stream is a Python component that reads data from a stream and filters it to only include a subset of the
data from the original request.

#### Configuration

Filter API stream has the following configuration parameters:

- PollIntervalSecs - The number of seconds to wait between polling the input stream. The default value is: `10` seconds.
- FilteredStreamName - The name of the stream where the filtered data goes. The default value: `FilteredStream`.
- DeviceList - The list of device IDs that should be included in the filtered output. The default value
  is: `[ "device_4", "device_7", "device_2" ]`

Filter API stream also has one special environment variable:

- ExtraConfigList - This value tells GGUtils to import the configuration from another component into this component's
  configuration. This allows the component to reuse the stream name that is specified in `com.greengrass.PollApi`without
  having to specify it in two places.

### ProcessApiStream

#### Description

Process API stream is a Python component that reads data from a stream and checks values in the data stream to see if
they meet some criteria. If the data does meet the criteria, the data is written to another stream.

#### Configuration

Process API stream has the following configuration parameters:

- PollIntervalSecs - The number of seconds to wait between polling the input stream. The default value is: `10` seconds.
- CheckFieldNumber - The index of the field in the JSON array that will be used to check if the data is above or below
  the max and min values. The default values is: `3`.
- CheckFieldMinValue - The minimum value for the check field. If a value is lower than this then the data will be put
  into the low value and the abnormal value streams. The default value is: `2`.
- CheckFieldMaxValue - The maximum value for the check field. If a value is higher than this then the data will be put
  into the high value and the abnormal value streams. The default value is: `8`.
- LowValueStreamName - The name of the stream where all low values are stored. The default value is: `LowValueStream`.
- HighValueStreamName - The name of the stream where all high values are stored. The default value is: `HighValueStream`
  .
- AbnormalValueStreamName - The name of the stream where all abnormal (high or low) values are stored. The default value
  is: `AbnormalValueStream`.

Process API stream also has one special environment variable:

- ExtraConfigList - This value tells GGUtils to import the configuration from another component into this component's
  configuration. This allows the component to reuse the filtered data stream name that is specified
  in `com.greengrass.FilterApiStream` without having to specify it in two places.

### ExportStreamToS3

#### Description

Export stream to S3 is a Python component that reads data from a stream, pulls out records from the stream in chunks,
writes those chunks to temporary files on disk, and then requests that Stream Manager exports the data to S3.

The export stream to S3 component creates two new streams to export data to S3 and track Stream Manager's progress.
Those stream names are based off of the PollApi stream name. For example, if the PollApi stream name
is `RawApiDataStream` then the export stream to S3 component will create two streams: `RawApiDataStreamExportRequests`
and `RawApiDataStreamExportStatus`.

Export stream to S3 will write into `RawApiDataStreamExportRequests` that data needs to be exported to S3. Stream
Manager will then write the status of that export into `RawApiDataStreamExportStatus`.

#### Configuration

NOTE: The configuration file for this component is called `recipes/com.greengrass.ExportStreamToS3-1.0.0.json.template`
because you **must** change the bucket name to match the name of your S3 bucket.

Export stream to S3 has the following configuration parameters:

- PollIntervalSecs - The number of seconds to wait between checking for new data in the stream. The default value
  is: `10` seconds.
- BucketName - The bucket name where the data will be exported. The Greengrass core **must** have write access to this
  bucket.
- KeyPrefix - The prefix to put on the names of objects that are exported to S3. Each object will have this prefix,
  followed by the sequence number of the first record in the file, a dash, and then the sequence number of the last
  record in the file. Each of the sequence numbers is an 8 digit decimal number with zeroes padded on the left. The
  default value is: `KeyPrefix-`.
- BatchSize - The number of records to read from the stream at a time. The default value is: `10`.

NOTE: Merges the configuration from PollApi with its own configuration so the stream name doesn't need to be specified
in both places.

Export stream to S3 also has one special environment variable:

- ExtraConfigList - This value tells GGUtils to import the configuration from another component into this component's
  configuration. This allows the component to reuse the stream name that is specified in `com.greengrass.PollApi`without
  having to specify it in two places.

### CleanUpExportFiles

#### Description

Clean up export files is a Python component that monitors the progress of Stream Manager exporting files to S3 and
cleans them up as necessary.

#### Configuration

Clean up export files to S3 also has one special environment variable:

- ExtraConfigList - This value tells GGUtils to import the configuration from another component into this component's
  configuration. This allows the component to reuse the stream name that is specified in `com.greengrass.PollApi`without
  having to specify it in two places.

## Rationale

### Why split the system into multiple components?

Splitting the polling and processing removes the risk that the data processing stage will fail and impact the polling
process. With separate components the data will still accumulate in stream manager even if the processing stage fails.

## Notes

### Debugging

The `aws.greengrass.LocalDebugConsole` component is deployed by default. To access this component you need to forward
ports 1441 and 1442 over SSH or have access to those ports on the system running Greengrass by some other method.

To connect to the dashboard it is easiest to use Firefox since the process for accepting self-signed certificates is
simpler. The steps to get connected to the dashboard using SSH port forwarding are as follows:

- If using SSH, forward ports 1441 and 1442 to localhost on your machine (
  e.g. `ssh user@host -L 1441:localhost:1441 -L 1442:localhost:1442`)
- Run `./greengrass/v2/bin/greengrass-cli get-debug-password` on the system running Greengrass. Copy the username and
  password provided.
- Open Firefox and navigate to `https://localhost:1442`, accept the warnings about the certificate to trust the
  self-signed certificate. This is the websocket port so you should expect to see a websocket upgrade error if this is
  successful.
- Open Firefox and navigate to `https://localhost:1441`, accept the warnings about the certificate to trust the
  self-signed certificate. Enter the username and password provided when you ran the `get-debug-password` command. You
  will now be connected to the dashboard.

### Python and localization

On the test systems used to implement this example the default locale did not work. If the locale is not supported then
you will see the following error:

```
RuntimeError: Click will abort further execution because Python was configured to use ASCII as encoding for the environment. Consult https://click.palletsprojects.com/unicode-support/ for mitigation steps.
```

This message comes from the [Click](https://click.palletsprojects.com/en/8.0.x/) library. Click is part of Flask so it
can not be removed.

If you run into this error you can add the following environment variables to the system:

```
LC_ALL=C.UTF-8
LANG=C.UTF-8
```

In the component configuration it would look like this:

```json
{
  "NOTE": "... SOME CONFIGURATION DATA OMITTED ...",
  "Manifests": [
    {
      "NOTE": "... SOME CONFIGURATION DATA OMITTED ...",
      "Lifecycle": {
        "Setenv": {
          "LC_ALL": "C.UTF-8",
          !
    [
      img.png
    ]
    (img.png)
    "LANG"
    :
    "C.UTF-8",
    "NOTE"
    :
    "... SOME CONFIGURATION DATA OMITTED ..."
    }
  }
  }
  ]
}
```

### GGUtils

GGUtils is based on
aws-greengrass-component-examples/machine-learning/sagemaker-edge-manager/artifacts/com.greengrass.SageMakerEdgeManager.ObjectDetection/1.0.0/object_detection/IPCUtils.py.
It has been converted into a component so it can be shared with other components.

### Architecture diagram

```text
┌────────────────┬─────────────────────────────────────────────────────────────────────────────────────────┐                      
│ AWS Greengrass │                                                                                         │                      
├────────────────┘                                                                                         │                      
│   ┌────────────┬─────────────────────────────────────────────────────────────────────────────────────┐   │                      
│   │ Deployment │                                                                                     │   │                      
│   ├────────────┘                                                                                     │   │                      
│   │ ┌────────────────────────────┐                                    ┌────────────────────────────┐ │   │                      
│   │ │      User components       │                                    │       AWS components       │ │   │      ┌──────────────┐
│   │ │   ┌───────────────────┐    │                                    │   ┌───────────────────┐    │ │   │      │              │
│   │ │   │ FakeAPI component │    │                                    │   │                   │    │ │   │      │              │
│   │ │   └───────────────────┘    │                                    │   │  Stream Manager   │    │ │   │      │  Amazon S3   │
│   │ │             │              │                                    │   │                   │────┼─┼───┼─────▶│              │
│   │ │             │              │    ┌────────────────────────────┐  │   │                   │    │ │   │      │              │
│   │ │             ▼              │    │          Streams           │  │   └───────────────────┘    │ │   │      │              │
│   │ │   ┌───────────────────┐    │    │    ┌──────────────────┐    │  │     ▲                      │ │   │      └──────────────┘
│   │ │   │ PollAPI component │────┼────┼───▶│                  │    │  │     │                      │ │   │                      
│   │ │   └───────────────────┘    │    │    │                  │    │  │     │                      │ │   │                      
│   │ │                            │    │    │ RawApiDataStream │    │  │     │                      │ │   │                      
│   │ │                            │    │    │                  │    │  │     │                      │ │   │                      
│   │ │   ┌───────────────────┐◀───┼────┼────│                  │    │  │     │                      │ │   │                      
│   │ │   │                   │    │    │    └──────────────────┘    │  └─────┼──────────────────────┘ │   │                      
│   │ │   │ ExportStreamToS3  │    │    └────────────────────────────┘  ┌─────┼──────────────────────┐ │   │                      
│   │ │   │     component     │────┼───────────────┐                    │     │   Filesystem         │ │   │                      
│   │ │   │                   │    │               │                    │   ┌──────────────────┐     │ │   │                      
│   │ │   └───────────────────┘    │               └────────────────────▶   │ Temp file 1 ...  │     │ │   │                      
│   │ │                            │                                    │   ├──────────────────┤     │ │   │                      
│   │ │                            │                                    │   │ Temp file 2 ...  │     │ │   │                      
│   │ │   ┌───────────────────┐    │               ┌────────────────────▶   └──────────────────┘     │ │   │                      
│   │ │   │                   │    │               │                    │                            │ │   │                      
│   │ │   │CleanUpExportFiles │    │               │                    │                            │ │   │                      
│   │ │   │     component     │────┼───────────────┘                    │                            │ │   │                      
│   │ │   │                   │    │                                    │                            │ │   │                      
│   │ │   └───────────────────┘    │                                    │                            │ │   │                      
│   │ │                            │                                    │                            │ │   │                      
│   │ │                            │                                    │                            │ │   │                      
│   │ │                            │                                    │                            │ │   │                      
│   │ │                            │                                    │                            │ │   │                      
│   │ │                            │                                    │                            │ │   │                      
│   │ │                            │                                    │                            │ │   │                      
│   │ └────────────────────────────┘                                    └────────────────────────────┘ │   │                      
│   └──────────────────────────────────────────────────────────────────────────────────────────────────┘   │                      
└──────────────────────────────────────────────────────────────────────────────────────────────────────────┘                      
```

C# cross-account publish on EC2 example
---------------------------------------

This example code contains two CloudFormation templates and one C# application. The C# application is meant to be executed on an EC2 instance that has the appropriate role attached to it.

partner-account.yaml is meant to be launched in the partner's account. It must be launched first so that the necessary IAM roles are created that are referenced by the customer account's template.

customer-account.yaml is meant to be launched in the customer's account. It must be launched second so that the necessary IAM roles are already present in the partner's account. If the partner template has not been launched this template will fail with an invalid principal error.

Customer account template
-------------------------
Contains:

- A cross-account role that can be assumed by the role in the partner account that allows the partner to call the [DescribeEndpoint API](https://docs.aws.amazon.com/cli/latest/reference/iot/describe-endpoint.html) and the [Publish API](https://docs.aws.amazon.com/cli/latest/reference/iot-data/publish.html) on any topic. The customer will need to specify the partner account ID as a parameter when they launch the template.

Partner account template
-------------------------
Contains:

- An AWS IoT Rules Engine rule that receives the data published by the customer on the `cross_account_hooks` topic hierarchy, adds the customer's account ID and operation to the payload, and invokes the Lambda function referenced below. The account ID and operation are added by the rules engine so they can't be spoofed by the customer by simply adding them to the payload. If the customer adds these values to the payload the rules engine will overwrite them.

- IAM role that allows the Lambda function to publish on any topic

- Lambda function that receives the payload from the AWS IoT Rules Engine, checks to make sure it passes a few basic checks (account ID, operation, and thing name present) and then republishes the event on a debug topic. It is up to the partner to do something useful with this information. It is also up to the partner to determine if they want to monitor other events (UpdateThing, etc) to keep their systems in sync with the customer's account.

- Lambda function version so that the function can be referenced by a specific version number

- Lambda permissions to allow the AWS IoT Rules Engine to invoke the Lambda function

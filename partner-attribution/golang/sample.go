package main

import (
	"fmt"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/request"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/iot"
	"github.com/aws/aws-sdk-go/service/iotdataplane"
)

func main() {
	// Create a session so we can create the IoT service client
	sess := session.Must(session.NewSession())

	// Select us-east-1 as the region that our IoT and IoT data clients will operate in
	awsRegion := "us-east-1"

	// Get the IoT control plane client
	iotcontrol := iot.New(sess, &aws.Config{Region: aws.String(awsRegion)})

	// Request the information about the Amazon Trust Services signed IoT data endpoint
	describeEndpointInput := &iot.DescribeEndpointInput{EndpointType: aws.String("IoT:Data-ATS")}
	endpoint, err := iotcontrol.DescribeEndpoint(describeEndpointInput)

	if err != nil {
		// Print errors, if any, and bail out
		fmt.Println(err.Error())
		return
	}

	// Get the IoT data plane client for the ATS endpoint
	iotdata := iotdataplane.New(sess, &aws.Config{Region: aws.String(awsRegion), Endpoint: endpoint.EndpointAddress})

	// Specify the platform name value (this must match the APN regex or it will be ignored by AWS - "^APN\\/1\\s((\\w){1,64}),(\\w{1,64})(,(([\\w\\.]){1,8}))?$")
	platformname := "APN/1 GolangPartnerSoft,ManagedIoT,v1.2.1"

	// Add the platform information to the HTTPS headers
	iotdata.Handlers.Send.PushFront(func(r *request.Request) {
		r.HTTPRequest.Header.Set("x-amzn-platform", platformname)
	})

	// Build and publish a test payload
	publishInput := &iotdataplane.PublishInput{
		Payload: []byte("payload_from_golang_sdk"),
		Qos:     aws.Int64(0),
		Topic:   aws.String("topic_from_golang_sdk"),
	}
	_, err = iotdata.Publish(publishInput)

	if err != nil {
		// Print errors, if any, and bail out
		fmt.Println(err.Error())
		return
	}
}

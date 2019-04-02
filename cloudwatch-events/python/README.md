CloudWatch Events Python example
--------------------------------

This example code contains two CloudFormation templates and two Python functions. The Python functions are referenced in the CloudFormation templates and are not meant to be called directly.

To launch these CloudFormation templates you must use the [CloudFormation package](https://docs.aws.amazon.com/cli/latest/reference/cloudformation/package.html) feature. This feature takes the local Python functions, uploads them to S3, and then creates new templates with the correct reference to the Python code in S3. This was done so the code can be maintained outside of the template easily.

customer-account.yaml is meant to be launched in the customer's account. It must be launched first so that the necessary IAM roles are created that are referenced by the partner account's template.

partner-account.yaml is meant to be launched in the partner's account. It must be launched second so that the necessary IAM roles are already present in the customer account. If the customer template has not been launched this template will fail with an invalid principal error.

Customer account template
-------------------------
Contains:

- CloudWatch Events rule that is triggered by [CreateThing API](https://docs.aws.amazon.com/iot/latest/apireference/API_CreateThing.html) calls. This will be triggered if the API is called from the SDKs, the CLI, or the console. The rule action triggers the Lambda function referenced below.

- IAM role that allows the Lambda function to assume the cross-account publish role in the partner's account and to describe any thing in the customer's account. The customer will need to specify the partner account ID as a parameter when they launch the template.

- Lambda function that receives the CreateThing API call, calls the [DescribeThing API](https://docs.aws.amazon.com/iot/latest/apireference/API_DescribeThing.html) to get additional information about the thing that was created, and then cross-account publishes the information to the partner account.

- Lambda function version so that the function can be referenced by a specific version number

- Lambda permissions to allow CloudWatch Events to invoke the Lambda function

Partner account template
-------------------------
Contains:

- An AWS IoT Rules Engine rule that receives the data published by the customer on the `cross_account_hooks` topic hierarchy, adds the customer's account ID and operation to the payload, and invokes the Lambda function referenced below. The account ID and operation are added by the rules engine so they can't be spoofed by the customer by simply adding them to the payload. If the customer adds these values to the payload the rules engine will overwrite them.

- IAM role that allows the Lambda function to publish on any topic

- Lambda function that receives the payload from the AWS IoT Rules Engine, checks to make sure it passes a few basic checks (account ID, operation, and thing name present) and then republishes the event on a debug topic. It is up to the partner to do something useful with this information. It is also up to the partner to determine if they want to monitor other events (UpdateThing, etc) to keep their systems in sync with the customer's account.

- Lambda function version so that the function can be referenced by a specific version number

- Lambda permissions to allow the AWS IoT Rules Engine to invoke the Lambda function

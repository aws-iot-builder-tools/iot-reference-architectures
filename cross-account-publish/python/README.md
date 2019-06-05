Cross-account publish Python example
------------------------------------

This example code contains two CloudFormation templates and one Python function. The Python function is referenced in the CloudFormation template and is not meant to be called directly.

To launch the partner CloudFormation templates you must use the [CloudFormation package](https://docs.aws.amazon.com/cli/latest/reference/cloudformation/package.html) feature. This feature takes the local Python function, uploads it to S3, and then creates a new template with the correct reference to the Python code in S3. This was done so the code can be maintained outside of the template easily.

partner-account.yaml is meant to be launched in the partner's account. It must be launched first so that the necessary IAM roles are created that are referenced by the customers account's template.

customer-account.yaml is meant to be launched in the customer's account. It must be launched second so that the necessary IAM roles are already present in the partner's account. If the partner template has not been launched this template will fail with an invalid principal error.

Customer account template
-------------------------
Contains:

- IAM role that allows the partner's Lambda function to assume it and cross-account publish into the `cross_account_publish` topic hierarchy. The customer will need to specify the partner account ID as a parameter when they launch the template.

Partner account template
-------------------------
Contains:

- An AWS IoT Rules Engine rule that receives the data published by the partner on the `cross_account_publish` topic hierarchy, adds the customer's account ID and topic information to the payload, and invokes the Lambda function referenced below.

- IAM role that allows the Lambda function to assume the cross-account role in the customer's account and access CloudWatch Logs in the partner account

- Lambda function that receives the payload from the AWS IoT Rules Engine, checks to make sure it passes a basic check (topic present) and then republishes the event on into the customer's account on a new topic with the partner's account ID. It is up to the customer to do something useful with this information.

- Lambda function version so that the function can be referenced by a specific version number

- Lambda permissions to allow the AWS IoT Rules Engine to invoke the Lambda function

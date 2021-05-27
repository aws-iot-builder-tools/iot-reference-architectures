package com.awssamples.dynamodb_api;

import com.aws.samples.cdk.constructs.iam.permissions.kinesisfirehose.kinesis.resources.ImmutableKinesisFirehoseStream;
import com.aws.samples.cdk.constructs.iam.permissions.kinesisfirehose.kinesis.resources.KinesisFirehoseStream;
import com.aws.samples.cdk.helpers.LambdaHelper;
import com.awslabs.cloudformation.data.ImmutableStackName;
import com.awslabs.cloudformation.data.ResourceType;
import com.awslabs.cloudformation.data.StackName;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.awslabs.dynamodb.data.ImmutableTableName;
import com.awslabs.dynamodb.data.TableName;
import com.awslabs.dynamodb.interfaces.DynamoDbHelper;
import com.awslabs.general.helpers.interfaces.LambdaPackagingHelper;
import com.awslabs.s3.helpers.data.ImmutableS3Bucket;
import com.awslabs.s3.helpers.data.S3Bucket;
import com.awssamples.MasterApp;
import com.awssamples.stacktypes.PythonStack;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableAttributes;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.kinesisfirehose.CfnDeliveryStream;
import software.amazon.awscdk.services.kinesisfirehose.CfnDeliveryStreamProps;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSourceProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import javax.inject.Inject;
import java.nio.file.Path;

import static com.aws.samples.cdk.constructs.iam.policies.KinesisFirehosePolicies.getKinesisFirehosePutRecordPolicyStatement;
import static com.aws.samples.cdk.constructs.iam.policies.S3Policies.getPutObjectPolicyStatementForBucket;
import static com.aws.samples.cdk.helpers.RoleHelper.buildRoleAssumedByFirehose;
import static com.aws.samples.cdk.helpers.RoleHelper.buildRoleAssumedByLambda;

public class DynamoDbApiDependentStack extends Stack implements PythonStack {
    public static final Logger log = LoggerFactory.getLogger(DynamoDbApiDependentStack.class);
    public static final String STACK_PREFIX = "dynamodb-api";
    public static final String PARENT_STACK_NAME = String.join("-", STACK_PREFIX, "stack");

    @Inject
    CloudFormationHelper cloudFormationHelper;
    @Inject
    DynamoDbHelper dynamoDbHelper;
    @Inject
    LambdaPackagingHelper lambdaPackagingHelper;

    public DynamoDbApiDependentStack(final Construct parent, final String name) {
        super(parent, name);

        // Inject dependencies
        MasterApp.masterInjector.inject(this);

        // Make sure the required "parent" stack exists. This is not a nested stack so it isn't a true "parent".
        StackName parentStackName = ImmutableStackName.builder().stackName(PARENT_STACK_NAME).build();

        if (!cloudFormationHelper.stackExists(parentStackName)) {
            throw new RuntimeException("Pre-requisite stack [" + PARENT_STACK_NAME + "] does not exist or is not in the CREATE_COMPLETE or UPDATE_COMPLETE state. Launch the stack in the DynamoDB API directory with CDK and try again.");
        }

        Stream<StackResourceSummary> parentStackResourceStream = cloudFormationHelper.getStackResources(parentStackName);
        Stream<StackResourceSummary> dynamoDbTableStream = cloudFormationHelper.filterStackResources(parentStackResourceStream, List.of(ResourceType.AWS_DynamoDB_Table));

//        dynamoDbTableStream.forEach(value -> log.info("physical: " + value.physicalResourceId()));
//        dynamoDbTableStream.forEach(value -> log.info("logical: " + value.logicalResourceId()));

//        @NotNull ITable myTable = Table.fromTableName(this, "dynamodbtable", dynamoDbTableStream.get().physicalResourceId());
//        @NotNull ITable myTable = Table.fromTableArn(this, "dynamodbtable", "arn:aws:dynamodb:us-east-1:541589084637:table/dynamodb-api-stack-DynamoDbTable6316879D-EI6AE474ED3E");
//        @NotNull TableAttributes xxx = TableAttributes.builder()
//                .tableArn("arn:aws:dynamodb:us-east-1:541589084637:table/dynamodb-api-stack-DynamoDbTable6316879D-1NOJGUN0BQLCZ")
//                .tableStreamArn("arn:aws:dynamodb:us-east-1:541589084637:table/dynamodb-api-stack-DynamoDbTable6316879D-1NOJGUN0BQLCZ/stream/2021-05-28T02:28:42.290")
//                .build();
        TableName tableName = ImmutableTableName.builder().tableName(dynamoDbTableStream.get().physicalResourceId()).build();
        @NotNull TableAttributes tableAttributes = tableNameToTableAttributes(tableName);
        @NotNull ITable sourceTable = Table.fromTableAttributes(this, "SourceTable", tableAttributes);

        // Build all of the necessary code
        Path deploymentPackage = build(this, lambdaPackagingHelper);

//        log.info("Table stream ARN: " + sourceTable.getTableStreamArn());

        BucketProps bucketProps = BucketProps.builder()
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        Bucket bucket = new Bucket(this, String.join("", name, "Bucket"), bucketProps);

        S3Bucket s3Bucket = ImmutableS3Bucket.builder().bucket(bucket.getBucketName()).build();
        PolicyStatement putS3ObjectPolicy = getPutObjectPolicyStatementForBucket(s3Bucket);
        Role firehoseRole = buildRoleAssumedByFirehose(this, "FirehoseRole", List.of(putS3ObjectPolicy), List.empty());

        CfnDeliveryStream.BufferingHintsProperty bufferingHintsProperty = CfnDeliveryStream.BufferingHintsProperty.builder()
                .intervalInSeconds(60)
                .sizeInMBs(5)
                .build();

        CfnDeliveryStream.CloudWatchLoggingOptionsProperty cloudWatchLoggingOptionsProperty = CfnDeliveryStream.CloudWatchLoggingOptionsProperty.builder()
                .enabled(true)
                .logGroupName(name)
                .logStreamName(name)
                .build();

        CfnDeliveryStream.S3DestinationConfigurationProperty s3DestinationConfigurationProperty = CfnDeliveryStream.S3DestinationConfigurationProperty.builder()
                .bucketArn(bucket.getBucketArn())
                .prefix(name)
                .roleArn(firehoseRole.getRoleArn())
                .bufferingHints(bufferingHintsProperty)
                .cloudWatchLoggingOptions(cloudWatchLoggingOptionsProperty)
                .build();

        KinesisFirehoseStream kinesisFirehoseStream = ImmutableKinesisFirehoseStream.builder()
                .name(String.join("", name, "Stream"))
                .build();

        CfnDeliveryStreamProps cfnDeliveryStreamProps = CfnDeliveryStreamProps.builder()
                .s3DestinationConfiguration(s3DestinationConfigurationProperty)
                .deliveryStreamName(kinesisFirehoseStream.getName())
                .build();

        CfnDeliveryStream cfnDeliveryStream = new CfnDeliveryStream(this, "DeliveryStreamName", cfnDeliveryStreamProps);

        DynamoEventSourceProps dynamoEventSourceProps = DynamoEventSourceProps.builder()
                .startingPosition(StartingPosition.TRIM_HORIZON)
                .batchSize(1)
                .bisectBatchOnError(true)
                .retryAttempts(3)
                .build();

        DynamoEventSource dynamoEventSource = new DynamoEventSource(sourceTable, dynamoEventSourceProps);

        Map<String, String> lambdaEnvironment = HashMap.of("delivery_stream_name", cfnDeliveryStream.getDeliveryStreamName());

        FunctionProps.Builder lambdaFunctionPropsBuilder = FunctionProps.builder()
                .runtime(Runtime.PYTHON_3_8)
                .memorySize(128)
                .timeout(Duration.seconds(10));

        PolicyStatement putFirehoseRecordPolicy = getKinesisFirehosePutRecordPolicyStatement(kinesisFirehoseStream);
        Role lambdaRole = buildRoleAssumedByLambda(this, "LambdaRole", List.of(putFirehoseRecordPolicy), List.empty());

        Function dynamoDbEventHandler = LambdaHelper.buildLambda(this, "DynamoDbEventHandler", lambdaRole, lambdaEnvironment, deploymentPackage.toString(), "lambda.function_handler", Option.of(lambdaFunctionPropsBuilder));
        dynamoDbEventHandler.addEventSource(dynamoEventSource);
    }

    private TableAttributes tableNameToTableAttributes(TableName tableName) {
        return dynamoDbHelper.tryDescribeTable(tableName)
                .map(this::tableDescriptionToTableAttributes)
                .get();
    }

    private TableAttributes tableDescriptionToTableAttributes(TableDescription tableDescription) {
        return TableAttributes.builder()
                .tableArn(tableDescription.tableArn())
                .tableStreamArn(tableDescription.latestStreamArn())
                .build();
    }

    @Override
    public String getProjectDirectory() {
        return "../dynamodb-api/" + getStackName();
    }
}

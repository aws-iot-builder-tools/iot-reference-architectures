package io.vertx.fargate;

import com.awssamples.fargate.IotCoreProxyStack;
import io.vavr.Lazy;
import io.vavr.collection.List;
import io.vavr.control.Option;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import static com.awssamples.fargate.IotCoreProxyStack.STACK_NAME_ENVIRONMENT_VARIABLE;

public class Helper {
    public static final Lazy<String> stackNameLazy = Lazy.of(Helper::getStackName);

    public static String getStackName() {
        return Option.of(System.getenv(STACK_NAME_ENVIRONMENT_VARIABLE))
                .orElse(() -> IotCoreProxyStack.stackNameOption)
                .getOrElseThrow(() -> new RuntimeException("Unable to determine the stack name"));
    }

    public static Stack getCloudFormationStack() {
        // Get the NLB endpoint from CloudFormation
        CloudFormationClient cloudformationClient = CloudFormationClient.create();

        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder()
                .stackName(stackNameLazy.get())
                .build();
        DescribeStacksResponse describeStacksResponse = cloudformationClient.describeStacks(describeStacksRequest);

        int numberOfStacks = describeStacksResponse.stacks().size();

        if (numberOfStacks == 0) {
            throw new RuntimeException("No stacks found with the specified name [" + stackNameLazy.get() + "]");
        }

        if (numberOfStacks != 1) {
            throw new RuntimeException("More than one stack found with the specified name [" + stackNameLazy.get() + "]");
        }

        return describeStacksResponse.stacks().get(0);
    }

    public static final String AUTHENTICATION_FUNCTION_LIST_ENVIRONMENT_VARIABLE = "AuthenticationFunctionList";
    public static List<String> AUTHENTICATION_FUNCTION_LIST = Option.of(System.getenv(AUTHENTICATION_FUNCTION_LIST_ENVIRONMENT_VARIABLE))
            .map(string -> string.split(","))
            .map(List::of)
            .getOrElse(List.empty());
}

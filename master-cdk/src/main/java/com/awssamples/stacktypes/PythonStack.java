package com.awssamples.stacktypes;

import com.awslabs.general.helpers.interfaces.LambdaPackagingHelper;
import com.awslabs.lambda.data.FunctionName;
import com.awslabs.lambda.data.ImmutableFunctionName;
import com.awslabs.lambda.data.PythonLambdaFunctionDirectory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.Stack;

import java.nio.file.Path;

public interface PythonStack {
    @NotNull PythonLambdaFunctionDirectory getPythonLambdaFunctionDirectory();

    default Logger getLogger() {
        return LoggerFactory.getLogger(PythonStack.class.getName());
    }

    default Path build(Stack stack, LambdaPackagingHelper lambdaPackagingHelper) {
        FunctionName functionName = ImmutableFunctionName.builder().name(stack.getStackName()).build();
        PythonLambdaFunctionDirectory pythonLambdaFunctionDirectory = getPythonLambdaFunctionDirectory();
        return lambdaPackagingHelper.packagePythonFunction(functionName, pythonLambdaFunctionDirectory);
    }

    String getProjectDirectory();
}

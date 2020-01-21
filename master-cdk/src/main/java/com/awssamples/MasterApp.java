package com.awssamples;

import com.awssamples.shared.CdkHelper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.jsii.JsiiException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MasterApp {
    private static final Logger log = LoggerFactory.getLogger(MasterApp.class);

    public static void main(final String argv[]) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (argv.length != 2) {
            throw new RuntimeException("Two arguments are required. The first argument is the class to use for the stack. The second argument is the name to use for the stack.");
        }

        String className = argv[0];
        String stackName = argv[1];

        CdkHelper.setStackName(stackName);

        Class stackClass = Class.forName(className);
        Constructor stackClassConstructor = stackClass.getConstructor(Construct.class, String.class);

        App app = new App();
        Try.of(() -> stackClassConstructor.newInstance(app, stackName))
                .onFailure(InvocationTargetException.class, MasterApp::logPossibleVersionIssue)
                .onFailure(JsiiException.class, MasterApp::logPossibleVersionIssue);

        app.synth();
    }

    private static void logPossibleVersionIssue(Exception exception) {
        exception.printStackTrace();

        log.error("Failed to create a CDK stack, this may be due to CDK being out of date. Try running 'npm i -g aws-cdk' and then re-run the stack. The complete exception information is above.");
    }
}

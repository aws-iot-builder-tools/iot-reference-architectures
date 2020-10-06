package com.awssamples;

import com.awslabs.general.helpers.data.ProcessOutput;
import com.awslabs.general.helpers.interfaces.ProcessHelper;
import com.awssamples.shared.CdkHelper;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.jsii.JsiiException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class MasterApp {
    private static final Logger log = LoggerFactory.getLogger(MasterApp.class);
    public static final MasterInjector masterInjector = DaggerMasterInjector.create();
    private static final ProcessHelper processHelper = masterInjector.processHelper();

    public static void main(final String argv[]) {
        if (argv.length != 2) {
            throw new RuntimeException("Two arguments are required. The first argument is the class to use for the stack. The second argument is the name to use for the stack.");
        }

        if (!isCorrectNodeJsVersion()) {
            throw new RuntimeException("CDK requires NodeJS version must be 12.x. Change the default version of NodeJS to a 12.x version and try again. [https://github.com/aws/aws-cdk/issues/5187]");
        }

        String className = argv[0];
        String stackName = argv[1];

        CdkHelper.setStackName(stackName);

        Try<? extends Class<?>> tryStackClass = Try.of(() -> Class.forName(className))
                .onFailure(ClassNotFoundException.class, MasterApp::logThrowable);

        if (tryStackClass.isFailure()) {
            log.error("Failed to get the stack class, exiting.");
            exitWithFailure();
        }

        Class stackClass = tryStackClass.get();

        Try<Constructor> tryStackClassConstructor = Try.of(() -> stackClass.getConstructor(Construct.class, String.class))
                .onFailure(NoSuchMethodException.class, MasterApp::logThrowable);

        if (tryStackClassConstructor.isFailure()) {
            log.error("Failed to get the stack class constructor, exiting.");
            exitWithFailure();
        }

        Constructor stackClassConstructor = tryStackClassConstructor.get();

        App app = new App();

        Try<Void> stackConstructorInvocation = Try.run(() -> stackClassConstructor.newInstance(app, stackName))
                .onFailure(InvocationTargetException.class, MasterApp::logPossibleVersionIssue)
                .onFailure(JsiiException.class, MasterApp::logPossibleVersionIssue);

        if (stackConstructorInvocation.isFailure()) {
            log.error("Failed to invoke the stack class constructor, exiting.");
            log.error(stackConstructorInvocation.getCause().toString());
            exitWithFailure();
        }

        app.synth();
    }

    private static boolean isCorrectNodeJsVersion() {
        List<String> programAndArguments = List.of("node", "--version");

        ProcessBuilder processBuilder = processHelper.getProcessBuilder(programAndArguments);

        Optional<ProcessOutput> optionalProcessOutput = processHelper.getOutputFromProcess(processBuilder);

        if (!optionalProcessOutput.isPresent()) {
            return false;
        }

        List<String> stdoutStrings = optionalProcessOutput.get().getStandardOutStrings();

        // We expect NodeJS 12.x only!
        return stdoutStrings.getOrElse("").startsWith("v12.");
    }

    private static void exitWithFailure() {
        System.exit(1);
    }

    private static void logThrowable(Throwable throwable) {
        log.error("Exception: " + throwable);
    }

    private static void logPossibleVersionIssue(Exception exception) {
        exception.printStackTrace();
        log.error("Failed to create a CDK stack, this may be due to CDK being out of date. Try running 'npm i -g aws-cdk' and then re-run the stack. The complete exception information is above.");
    }
}

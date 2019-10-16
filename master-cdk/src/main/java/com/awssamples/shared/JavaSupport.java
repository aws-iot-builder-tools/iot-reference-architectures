package com.awssamples.shared;

import io.vavr.control.Try;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class JavaSupport extends software.amazon.awscdk.core.Stack {
    private static final Logger log = LoggerFactory.getLogger(JavaSupport.class);

    public static void buildJar(File projectDirectoryFile) {
        ProjectConnection projectConnection = GradleConnector.newConnector()
                .forProjectDirectory(projectDirectoryFile)
                .connect();

        Try.withResources(() -> projectConnection)
                .of(JavaSupport::runBuild)
                .get();
    }

    public static Void runBuild(ProjectConnection projectConnection) {
        // Build with gradle and send the output to stderr
        BuildLauncher build = projectConnection.newBuild();
        build.forTasks("build");
        build.setStandardOutput(System.err);
        build.run();

        return null;
    }
}

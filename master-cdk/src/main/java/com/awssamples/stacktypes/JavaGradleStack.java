package com.awssamples.stacktypes;

import com.aws.samples.lambda.servlet.automation.GeneratedClassFinder;
import com.aws.samples.lambda.servlet.automation.GeneratedClassInfo;
import com.awssamples.gradle.GradleSupport;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Code;

import java.io.File;
import java.util.List;
import java.util.jar.JarFile;

import static com.awssamples.shared.CdkHelper.NO_SEPARATOR;
import static java.util.Collections.singletonList;

public interface JavaGradleStack {
    String getProjectDirectory();

    default List<File> getProjectDirectoryFiles() {
        return singletonList(new File(getProjectDirectory()));
    }

    default String getBuildOutputDirectory() {
        return "build/libs/";
    }

    String getOutputArtifactName();

    default Logger getLogger() {
        return LoggerFactory.getLogger(JavaGradleStack.class.getName());
    }

    default void build() {
        getLogger().info("Building artifacts...");
        getProjectDirectoryFiles().forEach(GradleSupport::buildJar);
        getLogger().info("Artifacts built");
    }

    default String getOutputArtifactRelativePath() {
        return String.join(NO_SEPARATOR, getProjectDirectory(), getBuildOutputDirectory(), getOutputArtifactName());
    }

    default AssetCode getAssetCode() {
        return Code.fromAsset(getOutputArtifactRelativePath());
    }

    default File getOutputArtifactFile() {
        return new File(getOutputArtifactRelativePath());
    }

    default JarFile getArtifactFile() {
        return Try.of(() -> new JarFile(new File(getOutputArtifactRelativePath()))).get();
    }

    default List<GeneratedClassInfo> getGeneratedClassInfo() {
        GeneratedClassFinder generatedClassFinder = new GeneratedClassFinder();
        return generatedClassFinder.getGeneratedClassList(getArtifactFile());
    }
}

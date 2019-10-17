package com.awssamples.shared;

import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PythonSupport {
    private static final String PACKAGE_DIRECTORY = "package";
    private static final String REQUIREMENTS_TXT = "requirements.txt";
    private static final Logger log = LoggerFactory.getLogger(PythonSupport.class);

    private static List<Path> getDirectorySnapshot(Path directory) {
        return Try.of(() -> Files.list(directory).collect(Collectors.toList())).get();
    }

    private static Void deleteDirectory(File directory) throws IOException {
        FileUtils.deleteDirectory(directory);

        return null;
    }

    public static Path buildZip(File baseDirectory, String functionName) {
        // Determine the absolute path of the package directory
        File absolutePackageDirectory = new File(String.join("/", baseDirectory.getAbsolutePath(), PACKAGE_DIRECTORY));

        // Determine what the output ZIP file name will be
        String zipFileName = String.join(".", functionName, "zip");
        Path zipFilePath = baseDirectory.toPath().resolve(zipFileName);

        // Delete any existing package directory
        cleanUpPackageDirectory(absolutePackageDirectory);

        // Delete any existing ZIP file
        Try.of(() -> Files.deleteIfExists(zipFilePath.toAbsolutePath())).get();

        // Get a snapshot of all of the files we need to copy to the
        List<Path> filesToCopyToPackageDirectory = getDirectorySnapshot(baseDirectory.toPath());

        // Install the requirements in a package directory
        List<String> programAndArguments = new ArrayList<>();
        programAndArguments.add("pip");
        programAndArguments.add("install");
        programAndArguments.add("-r");
        programAndArguments.add(REQUIREMENTS_TXT);
        programAndArguments.add("-t");
        programAndArguments.add(absolutePackageDirectory.getPath());

        ProcessBuilder processBuilder = ProcessHelper.getProcessBuilder(programAndArguments);
        processBuilder.directory(baseDirectory);

        List<String> stdoutStrings = new ArrayList<>();
        List<String> stderrStrings = new ArrayList<>();

        Optional<Integer> exitVal = ProcessHelper.getOutputFromProcess(log, processBuilder, true, Optional.of(stdoutStrings::add), Optional.of(stderrStrings::add));

        if (!exitVal.isPresent() || exitVal.get() != 0) {
            log.error("Something went wrong with pip, cannot continue");

            System.exit(1);
        }

        // Now the dependencies are in the directory, copy the rest of the necessary files in
        filesToCopyToPackageDirectory.forEach(file -> copyToDirectory(file, absolutePackageDirectory));

        // Package up everything into a deployment package ZIP file
        ZipUtil.pack(absolutePackageDirectory, zipFilePath.toFile());

        // Delete the package directory
        cleanUpPackageDirectory(absolutePackageDirectory);

        return zipFilePath;
    }

    private static void cleanUpPackageDirectory(File absolutePackageDirectory) {
        // Delete any existing package directory
        Try.of(() -> deleteDirectory(absolutePackageDirectory)).get();
    }

    private static void copyToDirectory(Path path, File destination) {
        File file = path.toFile();

        if (file.isDirectory()) {
            Try.of(() -> copyDirectory(file, destination)).get();
        } else {
            Try.of(() -> copyToDirectory(file, destination)).get();
        }
    }

    private static Void copyDirectory(File srcDir, File destDir) throws IOException {
        FileUtils.copyDirectory(srcDir, destDir);

        return null;
    }

    private static Void copyToDirectory(File srcDir, File destDir) throws IOException {
        FileUtils.copyToDirectory(srcDir, destDir);

        return null;
    }
}

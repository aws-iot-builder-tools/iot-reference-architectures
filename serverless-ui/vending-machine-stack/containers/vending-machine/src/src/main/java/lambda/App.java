package lambda;

import awslabs.client.shared.RaspberryPiRequest;
import awslabs.client.shared.RaspberryPiSettings;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.awslabs.general.helpers.implementations.JacksonHelper;
import com.awslabs.general.helpers.implementations.StreamHelper;
import com.awslabs.s3.helpers.data.ImmutableS3Bucket;
import com.awslabs.s3.helpers.data.ImmutableS3Key;
import com.awslabs.s3.helpers.data.S3Bucket;
import com.awslabs.s3.helpers.data.S3Key;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import com.redhat.et.libguestfs.GuestFS;
import io.vavr.Function0;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import static lambda.GuestFSHelper.mount;
import static lambda.GuestFSHelper.unmount;
import static lambda.SharedHelpers.*;

public class App implements RequestStreamHandler {
    private static final boolean debug = false;

    public static final java.util.HashMap<String, Object> READ_WRITE_PERMISSIONS = HashMap.<String, Object>of(
            "readonly", Boolean.FALSE,
            "format", "raw").toJavaMap();
    public static final String RASPIOS_BULLSEYE_ARMHF_LITE = "2022-04-04-raspios-bullseye-armhf-lite.img";
    public static final String S3_BUCKET = "s3Bucket";
    public static final S3Bucket s3Bucket = ImmutableS3Bucket.builder()
            .bucket(Option.of(System.getenv(S3_BUCKET))
                    .getOrElseThrow(() -> new RuntimeException(S3_BUCKET + " environment variable not present, cannot continue")))
            .build();
    public static final java.lang.String INITIALIZING_VIRTUAL_FILE_SYSTEM = "Initializing virtual file system";
    private static final Logger log = Logger.getLogger(App.class.getName());
    private static final String LOCAL_IMAGES_DIRECTORY_NAME = "/var/task/images";
    public static final String SOURCE_IMAGE_FILENAME = String.join("/", LOCAL_IMAGES_DIRECTORY_NAME, RASPIOS_BULLSEYE_ARMHF_LITE);
    private static final Lazy<Injector> lazyInjector = Lazy.of(DaggerInjector::create);
    private static final String LOCAL_SOURCE_IMAGE_FILENAME = String.join("/", LOCAL_IMAGES_DIRECTORY_NAME, RASPIOS_BULLSEYE_ARMHF_LITE);
    // ARM 32-bit SSM
    public static final String BASE_SSM_ARM32_FILENAME = "amazon-ssm-agent-arm32.deb";
    public static final String FULL_SSM_ARM32_FILENAME = String.join("/", LOCAL_IMAGES_DIRECTORY_NAME, BASE_SSM_ARM32_FILENAME);
    public static final File FULL_SSM_ARM32_FILE = new File(FULL_SSM_ARM32_FILENAME);
    // ARM 64-bit SSM
    public static final String BASE_SSM_ARM64_FILENAME = "amazon-ssm-agent-arm32.deb";
    public static final String FULL_SSM_ARM64_FILENAME = String.join("/", LOCAL_IMAGES_DIRECTORY_NAME, BASE_SSM_ARM64_FILENAME);
    public static final File FULL_SSM_ARM64_FILE = new File(FULL_SSM_ARM64_FILENAME);
    private static final File LOCAL_SOURCE_IMAGE_FILE = new File(LOCAL_SOURCE_IMAGE_FILENAME);
    private final java.util.HashMap<Long, AtomicInteger> counters = new java.util.HashMap<>();
    @Inject
    RaspberryPiSettingsProcessor raspberryPiSettingsProcessor;
    @Inject
    Provider<S3Helper> s3HelperProvider;

    @Inject
    public App() {
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        HashMap<String, Object> returnValue = innerHandleRequest(inputStream);

        returnValue = HashMap.of("statusCode", 200,
                "body", JacksonHelper.tryToJsonString(returnValue.toJavaMap()).get(),
                "isBase64Encoded", false);
        String json = JacksonHelper.tryToJsonString(returnValue.toJavaMap()).get();

        lambda.OutputStreamHelper.println(outputStream, json);
        Try.run(outputStream::close).get();
    }

    private HashMap<String, Object> logInfo(HashMap<String, Object> returnValue, String message) {
        info(message);
        return addMessageToMap(returnValue, "info", message);
    }

    private HashMap<String, Object> addMessageToMap(HashMap<String, Object> returnValue, String inputKey, String message) {
        return returnValue
                .computeIfAbsent(inputKey, unused -> List.empty())._2
                .computeIfPresent(inputKey, (key, value) -> ((List<String>) value).append(message))._2;
    }

    private HashMap<String, Object> logWarning(HashMap<String, Object> returnValue, String message) {
        warning(message);
        return addMessageToMap(returnValue, "warning", message);
    }

    public HashMap<String, Object> modifyImage(HashMap<String, Object> returnValue, RaspberryPiSettings raspberryPiSettings) {
        lazyInjector.get().inject(this);

        SharedHelpers.publishBuildStarted();

        String outputZipFilename = getOutputZipFilename(raspberryPiSettings);
        S3Key outputZipS3Key = ImmutableS3Key.builder().key(outputZipFilename).build();

        if (!s3HelperProvider.get().objectExists(s3Bucket, outputZipS3Key)) {
            // Image does not exist, build it
            info("Image does not exist, building");
            createNewBuildInRegistry();
            returnValue = buildImage(returnValue, raspberryPiSettings, outputZipFilename, outputZipS3Key);
        } else {
            info("Image exists, not building");
        }

        // Get a pre-signed URL that is valid for 20 minutes
        URL presignedS3Url = s3HelperProvider.get().presign(s3Bucket, outputZipS3Key, Duration.ofMinutes(20));

        publishBuildFinished(presignedS3Url);
        returnValue = logInfo(returnValue, "aws s3 cp s3://" + s3Bucket.bucket() + "/" + outputZipS3Key.key() + " processed-images/" + outputZipS3Key.key());

        String json = JacksonHelper.tryToJsonString(counters).get();
        returnValue = logInfo(returnValue, "counters: " + json);

        return returnValue;
    }

    @NotNull
    private HashMap<String, Object> buildImage(HashMap<String, Object> returnValue, RaspberryPiSettings raspberryPiSettings, String outputZipFilename, S3Key outputZipS3Key) {
        File compressedImageFile = new File(String.join("/", "/tmp", outputZipFilename));

        String uuidFilename = String.join(".", UUID.randomUUID().toString(), "img");
        String destinationImageFullPath = String.join("/", "/tmp", uuidFilename);
        File destinationImageFile = new File(destinationImageFullPath);
        String destinationImageFilename = destinationImageFile.getName();

        // Get the number of steps. There are 4 required steps and then some optional steps.
        SharedHelpers.setTotalSteps(4 + raspberryPiSettingsProcessor.getSteps(null, raspberryPiSettings).length());
        SharedHelpers.resetCurrentStep();

        if (!destinationImageFile.exists()) {
            info("Copying file because it does not exist");
            SharedHelpers.startNextStep("Creating temporary disk image");
            info("SOURCE: " + SOURCE_IMAGE_FILENAME);
            info("DEST: " + destinationImageFile.getAbsolutePath());
            MonitorFileInputStream monitorFileInputStream = Try.of(() -> new MonitorFileInputStream(new File(SOURCE_IMAGE_FILENAME))).get();
            Try.of(() -> Files.copy(monitorFileInputStream, destinationImageFile.toPath())).get();
            SharedHelpers.finishStep();
            info("Copied file");
        }

        SharedHelpers.startNextStep(INITIALIZING_VIRTUAL_FILE_SYSTEM);

        // Initialize and launch the GuestFS appliance
        GuestFS guestFS = getGuestFS(destinationImageFile);
        mount(guestFS);

        try {
            SharedHelpers.finishStep();

            // Get all of the steps
            List<Tuple2<String, Function0<Void>>> steps = raspberryPiSettingsProcessor.getSteps(guestFS, raspberryPiSettings);

            // Execute all of the steps
            steps.forEach(this::runStep);

            close(guestFS);

            SharedHelpers.startNextStep("Compressing image");

            returnValue = logInfo(returnValue, "Size before: " + LOCAL_SOURCE_IMAGE_FILE.length());

            ZipEntrySource[] zipEntrySourceArray = getZipEntrySourceArray(destinationImageFilename, destinationImageFile);

            ZipUtil.pack(zipEntrySourceArray, compressedImageFile);

            returnValue = logInfo(returnValue, "Size after: " + compressedImageFile.length());

            finishStep();

            SharedHelpers.startNextStep("Copying image to S3");
            MonitorFileInputStream monitorFileInputStream = Try.of(() -> new MonitorFileInputStream(compressedImageFile)).get();
            PutObjectResponse response = s3HelperProvider.get().copyToS3(s3Bucket, outputZipS3Key, monitorFileInputStream, Math.toIntExact(compressedImageFile.length()));
            SharedHelpers.finishStep();
            return returnValue;
        } finally {
            unmount(guestFS);
        }
    }

    @NotNull
    private ZipEntrySource[] getZipEntrySourceArray(String destinationImageFilename, File destinationImageFile) {
        ZipEntrySource zipEntrySource = new ZipEntrySource() {
            @Override
            public String getPath() {
                return destinationImageFilename;
            }

            @Override
            public ZipEntry getEntry() {
                return new ZipEntry(destinationImageFilename);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new MonitorFileInputStream(destinationImageFile);
            }
        };

        ZipEntrySource[] zipEntrySourceArray = new ZipEntrySource[1];
        zipEntrySourceArray[0] = zipEntrySource;
        return zipEntrySourceArray;
    }

    private void runStep(Tuple2<String, Function0<Void>> value) {
        SharedHelpers.startNextStep(value._1);
        // Execute the step
        value._2.get();
        SharedHelpers.finishStep();
    }

    private String calculateInputUniqueName(RaspberryPiSettings raspberryPiSettings) {
        String json = JacksonHelper.tryToJsonString(raspberryPiSettings).get();
        String hash = sha256Hash(json);

        if ((raspberryPiSettings.imageName == null) || (raspberryPiSettings.imageName.isEmpty())) {
            throw new RuntimeException("Image name cannot be NULL");
        }

        return String.join("-", raspberryPiSettings.imageName, hash.substring(0, 8));
    }


    @NotNull
    private String getOutputZipFilename(RaspberryPiSettings raspberryPiSettings) {
        return String.join(".", calculateInputUniqueName(raspberryPiSettings), "img", "zip");
    }

    private void close(GuestFS guestFS) {
        Try.run(guestFS::shutdown).get();
        Try.run(guestFS::close).get();
    }

    @NotNull
    private GuestFS getGuestFS(File destinationImage) {
        GuestFS guestFS = Try.of(GuestFS::new).get();

        if (debug) {
            enableDebugCallback(guestFS);
        }

        Try.run(() -> guestFS.set_cachedir("/tmp")).get();

        SharedHelpers.publishBuildProgress(25);

        Try.run(() -> guestFS.add_drive(destinationImage.getAbsolutePath(), READ_WRITE_PERMISSIONS)).get();
        // Try.run(() -> guestFS.set_verbose(true)).get();
        // Try.run(() -> guestFS.set_trace(true)).get();

        SharedHelpers.publishBuildProgress(50);

        Try.run(guestFS::launch).get();

        return guestFS;
    }

    private void enableDebugCallback(GuestFS guestFS) {
        info("Setting up callback");

        // 0x03ff captures all events
        Try.of(() -> guestFS.set_event_callback((l, i, s, longs) -> {
            info("Event callback: l [" + l + "], i [" + i + "]");
            info(s == null ? "NULL" : s);
            counters.computeIfAbsent(l, value -> new AtomicInteger(0));
            counters.get(l).incrementAndGet();
        }, 0x03ff)).get();

        info("Callback set up");
    }

    private HashMap<String, Object> innerHandleRequest(InputStream inputStream) {
        HashMap<String, Object> returnValue = HashMap.empty();

        lazyInjector.get().inject(this);

        String inputString = StreamHelper.inputStreamToString(inputStream);
        log.info("Input string BEFORE: " + inputString);

        inputString = JacksonHelper.tryParseJson(inputString, Map.class)
                // Pull the body out of the JSON payload, that's where out data is
                .map(map -> map.get("body"))
                // Cast it to a string
                .map(String.class::cast)
                .toOption()
                // If any of the previous steps failed just use the original string
                .getOrElse(inputString);

        log.info("Input string AFTER: " + inputString);

        Try<RaspberryPiRequest> raspberryPiRequestTry = JacksonHelper.tryParseJson(inputString, RaspberryPiRequest.class);

        if (raspberryPiRequestTry.isFailure()) {
            raspberryPiRequestTry.getCause().printStackTrace();
            return logWarning(returnValue, "Didn't get Raspberry Pi options [" + raspberryPiRequestTry.getCause().getMessage() + "]");
        }

        RaspberryPiRequest raspberryPiRequest = raspberryPiRequestTry.get();
        RaspberryPiSettings raspberryPiSettings = raspberryPiRequest.settings;

        String uniqueInputName = calculateInputUniqueName(raspberryPiSettings);
        SharedHelpers.setBuildId(uniqueInputName);

        if (raspberryPiRequest.dryRun) {
            return returnValue
                    .put("success", "Config looks good, dry run specified, no actions performed")
                    .put("settings", JacksonHelper.tryToJsonString(raspberryPiRequest).get())
                    .put("hash", uniqueInputName);
        }

        if (raspberryPiSettings.userId == null) {
            throw new RuntimeException("User ID cannot be NULL");
        }

        String clientId = sha256Hash(raspberryPiSettings.userId);
        SharedHelpers.setClientId(clientId);
        SharedHelpers.setUserId(raspberryPiSettings.userId);

        return modifyImage(returnValue, raspberryPiSettings);
    }
}

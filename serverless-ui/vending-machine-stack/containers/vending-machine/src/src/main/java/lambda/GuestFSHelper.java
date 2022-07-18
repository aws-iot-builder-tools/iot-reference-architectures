package lambda;

import com.redhat.et.libguestfs.GuestFS;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static lambda.SharedHelpers.info;

public class GuestFSHelper {
    public static final String BOOT_DISK_NAME = "/dev/sda1";
    public static final String ROOT_DISK_NAME = "/dev/sda2";
    public static final String ROOT_PATH = "/";
    public static final String BOOT_PATH = "/boot";
    // Index of the user ID value in the ETC_PASSWORD file
    public static final int USER_ID_INDEX = 2;
    // Index of the group ID value in the ETC_GROUP file
    public static final int GROUP_ID_INDEX = 2;
    public static final int MINIMUM_GROUP_ENTRY_LENGTH = 3;
    public static final String ETC_PASSWD = "/etc/passwd";
    public static final String ETC_GROUP = "/etc/group";
    private static final int MINIMUM_PASSWD_ENTRY_LENGTH = 4;

    @NotNull
    private static String getRootPath(String filename) {
        return String.join("/", ROOT_PATH, filename);
    }

    @NotNull
    private static String getBootPath(String filename) {
        return String.join("/", BOOT_PATH, filename);
    }

    public static void writeBootFile(GuestFS guestFS, String filename, String data) {
        writeFile(guestFS, getBootPath(filename), data);
    }

    private static void writeFile(GuestFS guestFS, String filename, String data) {
        Try.run(() -> guestFS.write(filename, data.getBytes(StandardCharsets.UTF_8))).get();
    }

    public static void appendRootFile(GuestFS guestFS, String filename, String data) {
        info("Appending data to file: " + filename);
        info("DATA: " + data);
        Try.run(() -> guestFS.write_append(getRootPath(filename), data.getBytes(StandardCharsets.UTF_8))).get();
    }

    public static void writeRootFile(GuestFS guestFS, String destinationFilename, File sourceFile) {
        byte[] bytes = Try.of(() -> Files.readAllBytes(sourceFile.toPath())).get();
        Try.run(() -> guestFS.write(destinationFilename, bytes)).get();
    }

    public static void chownRoot(GuestFS guestFS, String filename, int uid, int gid) {
        Try.run(() -> guestFS.chown(uid, gid, getRootPath(filename))).get();
    }

    public static void chmodRoot(GuestFS guestFS, String filename, int mode) {
        Try.run(() -> guestFS.chmod(mode, getRootPath(filename))).get();
    }

    private static void writeFile(GuestFS guestFS, String filename, List<String> lines) {
        String joinedLines = String.join("\n", lines);
        writeFile(guestFS, filename, joinedLines);
    }

    public static void touchBootFile(GuestFS guestFS, String filename) {
        Try.run(() -> guestFS.touch(getBootPath(filename))).get();
    }

    public static void mkdirRoot(GuestFS guestFS, String directoryName) {
        Try.run(() -> guestFS.mkdir_p(getRootPath(directoryName))).get();
    }

    private static List<String> readRootFileOrThrow(GuestFS guestFS, String filename) {
        return readRootFile(guestFS, filename).get();
    }

    public static Try<List<String>> readRootFile(GuestFS guestFS, String filename) {
        Try<List<String>> result = Try.of(() -> List.of(guestFS.read_lines(getRootPath(filename))));

        return result;
    }

    public static List<String> readBootFileOrThrow(GuestFS guestFS, String filename) {
        return readBootFile(guestFS, filename).get();
    }

    public static Try<List<String>> readBootFile(GuestFS guestFS, String filename) {
        Try<List<String>> result = Try.of(() -> List.of(guestFS.read_lines(getBootPath(filename))));

        return result;
    }

    private static void unmountBootPartition(GuestFS guestFS) {
        Try.run(() -> guestFS.umount(BOOT_PATH));
    }

    private static void unmountRootPartition(GuestFS guestFS) {
        Try.run(() -> guestFS.umount(ROOT_PATH));
    }

    public static void mount(GuestFS guestFS) {
        mountRootPartition(guestFS);
        mkdirRoot(guestFS, BOOT_PATH);
        mountBootPartition(guestFS);
    }

    public static void unmount(GuestFS guestFS) {
        unmountBootPartition(guestFS);
        unmountRootPartition(guestFS);
    }

    private static void mountBootPartition(GuestFS guestFS) {
        Try.run(() -> guestFS.mount(BOOT_DISK_NAME, BOOT_PATH)).get();
    }

    private static void mountRootPartition(GuestFS guestFS) {
        Try.run(() -> guestFS.mount(ROOT_DISK_NAME, ROOT_PATH)).get();
    }

    public static Option<Integer> getUserId(GuestFS guestFS, String username) {
        return getIdFromPasswordOrGroupFile(guestFS, ETC_PASSWD, username, MINIMUM_PASSWD_ENTRY_LENGTH, USER_ID_INDEX);
    }

    public static Option<Integer> getGroupId(GuestFS guestFS, String groupName) {
        return getIdFromPasswordOrGroupFile(guestFS, ETC_GROUP, groupName, MINIMUM_GROUP_ENTRY_LENGTH, GROUP_ID_INDEX);
    }

    private static Option<Integer> getIdFromPasswordOrGroupFile(GuestFS guestFS, String filename, String prefix, int minimumLength, int index) {
        return readRootFileOrThrow(guestFS, filename)
                .filter(line -> line.startsWith(prefix + ":"))
                .toOption()
                .map(line -> line.split(":"))
                .map(List::of)
                .filter(list -> list.length() >= minimumLength)
                .map(list -> list.get(index))
                .map(Integer::parseInt);
    }

    // NOTE: This function fails when running in Lambda, use writeFile instead
    /*
    public static void copyFile(GuestFS guestFS, String sourceFilename, String destinationDirectory) {
        Try.run(() -> guestFS.copy_in(sourceFilename, destinationDirectory)).get();
    }
     */

    // Index of the default group ID value in the ETC_GROUP file
//    public static final int USER_DEFAULT_GROUP_ID_INDEX = 3;
//    private static Option<Integer> getUserDefaultGroupId(GuestFS guestFS, String username) {
//        return getIdFromPasswordOrGroupFile(guestFS, ETC_PASSWD, username, MINIMUM_PASSWD_ENTRY_LENGTH, USER_DEFAULT_GROUP_ID_INDEX);
//    }

//    public static void writeRootFile(GuestFS guestFS, String filename, String data) {
//        writeFile(guestFS, getRootPath(filename), data);
//    }
}

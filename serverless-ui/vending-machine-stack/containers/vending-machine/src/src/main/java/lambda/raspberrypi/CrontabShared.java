package lambda.raspberrypi;

import com.redhat.et.libguestfs.GuestFS;

import static lambda.GuestFSHelper.*;
import static lambda.SharedHelpers.info;

public class CrontabShared {
    public static final String SHELL_AND_PATH_PREAMBLE = "\n" +
            "SHELL=/bin/bash\n" +
            "PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin\n";

    public static final String VAR_SPOOL_CRON_CRONTABS = "/var/spool/cron/crontabs";
    public static final String ROOT_USER_CRONTAB_PATH = String.join("/", VAR_SPOOL_CRON_CRONTABS, "root");

    public static void makeCrontabDirectory(GuestFS guestFS) {
        mkdirRoot(guestFS, VAR_SPOOL_CRON_CRONTABS);
    }

    public static void logRootCrontab(GuestFS guestFS, String prefix) {
        readRootFile(guestFS, ROOT_USER_CRONTAB_PATH)
                .onFailure(e -> info("Couldn't read " + ROOT_USER_CRONTAB_PATH + ": " + e.getMessage()))
                .forEach(line -> info(prefix + ": " + line));
    }

    public static boolean rootCrontabExists(GuestFS guestFS) {
        return readRootFile(guestFS, ROOT_USER_CRONTAB_PATH).isSuccess();
    }

    public static String addRootCrontabShellAndPathPreambleIfNecessary(GuestFS guestFS, String crontab) {
        if (rootCrontabExists(guestFS)) {
            return crontab;
        }

        return SHELL_AND_PATH_PREAMBLE + crontab;
    }

    public static void setRootCrontabPermissions(GuestFS guestFS) {
        int rootUserId = getUserId(guestFS, "root").getOrElseThrow(() -> new RuntimeException("Couldn't determine root's uid"));
        int crontabGroupId = getGroupId(guestFS, "crontab").getOrElseThrow(() -> new RuntimeException("Couldn't determine crontab's gid"));

        chownRoot(guestFS, ROOT_USER_CRONTAB_PATH, rootUserId, crontabGroupId);
        chmodRoot(guestFS, ROOT_USER_CRONTAB_PATH, 0600);
    }
}

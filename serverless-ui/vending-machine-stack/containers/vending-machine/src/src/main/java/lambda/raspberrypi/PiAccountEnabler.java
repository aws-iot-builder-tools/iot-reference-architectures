package lambda.raspberrypi;

import awslabs.client.shared.RaspberryPiSettings;
import com.redhat.et.libguestfs.GuestFS;
import io.vavr.Function0;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.logging.Logger;

import static lambda.GuestFSHelper.appendRootFile;
import static lambda.SharedHelpers.info;
import static lambda.raspberrypi.CrontabShared.*;

public class PiAccountEnabler implements GuestFsStepProvider<RaspberryPiSettings> {
    private static final Logger log = Logger.getLogger(PiAccountEnabler.class.getName());
    private static final String USERNAME = "pi";
    private static final String PASSWORD = "raspberry";

    @Inject
    public PiAccountEnabler() {
    }

    @Override
    public String getEnabledMessage() {
        return "Setting user " + USERNAME + " password " + PASSWORD;
    }

    @Override
    public String getDisabledMessage() {
        return "Not setting " + USERNAME + " password";
    }

    @Override
    public boolean willRun(RaspberryPiSettings raspberryPiSettings) {
        return raspberryPiSettings.addPiAccount;
    }

    @Override
    public Void enable(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings) {
        // Guidance from: https://stackoverflow.com/a/2409369/796579
        String crontab = "# On reboot:\n" +
                "# - If the " + USERNAME + " user does not have a bcrypted password, set their password to " + PASSWORD + "\n" +
                "@reboot fgrep '" + USERNAME + ":$' /etc/shadow || " +
                "(" +
                "echo '" + USERNAME + ":" + PASSWORD + "' | chpasswd" +
                ")\n";

        info("Mounting root partition");
        makeCrontabDirectory(guestFS);
        crontab = addRootCrontabShellAndPathPreambleIfNecessary(guestFS, crontab);
        logRootCrontab(guestFS, "BEFORE");
        appendRootFile(guestFS, ROOT_USER_CRONTAB_PATH, crontab);
        logRootCrontab(guestFS, "AFTER");
        setRootCrontabPermissions(guestFS);

        return null;
    }
}

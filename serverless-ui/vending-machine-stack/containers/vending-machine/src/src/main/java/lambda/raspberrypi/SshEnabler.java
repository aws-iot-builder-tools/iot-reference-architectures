package lambda.raspberrypi;

import awslabs.client.shared.RaspberryPiSettings;
import com.redhat.et.libguestfs.GuestFS;

import javax.inject.Inject;
import java.util.logging.Logger;

import static lambda.GuestFSHelper.touchBootFile;

public class SshEnabler implements GuestFsStepProvider<RaspberryPiSettings> {
    public static final String SSH_ENABLE = "/ssh";
    private static final Logger log = Logger.getLogger(SshEnabler.class.getName());

    @Inject
    public SshEnabler() {
    }

    @Override
    public String getEnabledMessage() {
        return "Enabling SSH";
    }

    @Override
    public String getDisabledMessage() {
        return "Not enabling SSH";
    }

    @Override
    public boolean willRun(RaspberryPiSettings raspberryPiSettings) {
        return raspberryPiSettings.sshEnabled;
    }

    @Override
    public Void enable(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings) {
        touchBootFile(guestFS, SSH_ENABLE);

        return null;
    }
}

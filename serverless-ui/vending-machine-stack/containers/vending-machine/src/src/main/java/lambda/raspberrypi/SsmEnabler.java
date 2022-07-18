package lambda.raspberrypi;

import awslabs.client.shared.RaspberryPiSettings;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.redhat.et.libguestfs.GuestFS;
import io.vavr.control.Option;
import lambda.App;
import lambda.SharedHelpers;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.CreateActivationRequest;
import software.amazon.awssdk.services.ssm.model.CreateActivationResponse;

import javax.inject.Inject;
import java.util.logging.Logger;

import static lambda.GuestFSHelper.appendRootFile;
import static lambda.GuestFSHelper.writeRootFile;
import static lambda.SharedHelpers.*;
import static lambda.raspberrypi.CrontabShared.*;

public class SsmEnabler implements GuestFsStepProvider<RaspberryPiSettings> {
    private static final Logger log = Logger.getLogger(SsmEnabler.class.getName());
    private static final String DESTINATION_DIRECTORY = "/root";
    public static final String SSM_ROLE_NAME = "ssmRoleName";
    @Inject
    IamHelper iamHelper;

    @Inject
    public SsmEnabler() {
    }

    @Override
    public boolean willRun(RaspberryPiSettings raspberryPiSettings) {
        return raspberryPiSettings.ssmEnabled;
    }

    @Override
    public String getEnabledMessage() {
        return "Enabling SSM";
    }

    @Override
    public String getDisabledMessage() {
        return "Not enabling SSM";
    }

    @Override
    public Void enable(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings) {
        String ssmRoleName = Option.of(System.getenv(SSM_ROLE_NAME))
                .getOrElseThrow(() -> new RuntimeException(SSM_ROLE_NAME + " environment variable not present, cannot continue"));

        CreateActivationRequest createActivationRequest = CreateActivationRequest.builder()
                .iamRole(ssmRoleName)
                .defaultInstanceName(SharedHelpers.getBuildId())
                .build();

        CreateActivationResponse createActivationResponse = SsmClient.create().createActivation(createActivationRequest);

        String activationCode = createActivationResponse.activationCode();
        String activationId = createActivationResponse.activationId();

        publishNewSystem(activationId);
        addAttributeToBuildInRegistry(ACTIVATION_ID, activationId);

        // Guidance from: https://stackoverflow.com/a/2409369/796579
        String crontab = "# On reboot:\n" +
                "# - If the amazon-ssm-agent isn't installed - install it and stop it\n" +
                "# - If the amazon-ssm-agent isn't registered - register it and start it\n" +
                "@reboot test -f \"/usr/bin/amazon-ssm-agent\" || " +
                "(" +
                "/usr/bin/dpkg -i " + DESTINATION_DIRECTORY + "/" + App.BASE_SSM_ARM32_FILENAME + " && " +
                "/usr/sbin/service amazon-ssm-agent stop" +
                ") && " +
                "test -f \"/var/lib/amazon/ssm/registration\" || " +
                "(" +
                "/usr/bin/amazon-ssm-agent -register -code \"" + activationCode + "\" -id \"" + activationId + "\" -region us-east-1 && " +
                "/usr/sbin/service amazon-ssm-agent start " +
                ")\n";

        writeRootFile(guestFS, String.join("/", DESTINATION_DIRECTORY, App.FULL_SSM_ARM32_FILE.getName()), App.FULL_SSM_ARM32_FILE);
        makeCrontabDirectory(guestFS);
        crontab = addRootCrontabShellAndPathPreambleIfNecessary(guestFS, crontab);
        appendRootFile(guestFS, ROOT_USER_CRONTAB_PATH, crontab);
        setRootCrontabPermissions(guestFS);

        return null;
    }
}

package lambda.raspberrypi;

import awslabs.client.shared.RaspberryPiSettings;
import com.redhat.et.libguestfs.GuestFS;

import javax.inject.Inject;
import java.util.logging.Logger;

import static lambda.GuestFSHelper.writeBootFile;
import static lambda.SharedHelpers.info;

public class WiFiEnabler implements GuestFsStepProvider<RaspberryPiSettings> {
    public static final String WPA_SUPPLICANT_CONF = "/wpa_supplicant.conf";
    private static final Logger log = Logger.getLogger(WiFiEnabler.class.getName());

    @Inject
    public WiFiEnabler() {
    }

    @Override
    public String getEnabledMessage() {
        return "Enabling WiFi";
    }

    @Override
    public String getDisabledMessage() {
        return "Not enabling WiFi";
    }

    @Override
    public boolean willRun(RaspberryPiSettings input) {
        // Tracking the return value instead of exiting early allows us to show in the logs if the SSID or password or both are missing
        boolean returnValue = true;

        if (input.getWifiSsidOption().isEmpty()) {
            info("No WiFi SSID provided, not configuring WiFi");
            returnValue = false;
        }

        if (input.getWifiPasswordOption().isEmpty()) {
            info("No WiFi password provided, not configuring WiFi");
            returnValue = false;
        }

        return returnValue;
    }

    @Override
    public Void enable(GuestFS guestFS, RaspberryPiSettings raspberryPiSettings) {
        String wifiSsid = raspberryPiSettings.wifiSsidNullable;
        String wifiPassword = raspberryPiSettings.wifiPasswordNullable;

        String output = "ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev\n" +
                "update_config=1\n" +
                "country=" + raspberryPiSettings.wifiCountryCode + "\n" +
                "network={\n" +
                " ssid=\"" + wifiSsid + "\"\n" +
                " psk=\"" + wifiPassword + "\"\n" +
                "}";
        writeBootFile(guestFS, WPA_SUPPLICANT_CONF, output);

        return null;
    }
}

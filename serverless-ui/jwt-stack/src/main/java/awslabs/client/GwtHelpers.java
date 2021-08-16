package awslabs.client;

import elemental2.dom.ClipboardEvent;
import elemental2.dom.DomGlobal;
import elemental2.dom.EventListener;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.collection.List;
import jsinterop.base.Js;

import java.util.logging.Logger;

public class GwtHelpers {
    private static final Logger log = Logger.getLogger("");

    public static void info(String message) {
        log.info(message);
    }

    public static String bytesToHexString(byte[] bytes, int length) {
        StringBuilder stringBuilder = new StringBuilder();

        int padding = length - bytes.length;

        for (int loop = 0; loop < padding; loop++) {
            stringBuilder.append("00");
        }

        List.ofAll(bytes)
                .map(GwtHelpers::singleByteToHexString)
                .forEach(stringBuilder::append);

        return stringBuilder.toString();
    }

    public static String singleByteToHexString(byte value) {
        return singleByteToHexString((int) value);
    }

    private static String singleByteToHexString(int value) {
        // Make sure we add a high bit so we don't lose leading zeroes
        value |= 0x100;

        // Convert to hex
        String fullHexString = Integer.toHexString(value);

        // Only grab the last two characters (for negative bytes values will be 0xffffff??, positive values will be 0x1??)
        return fullHexString.substring(fullHexString.length() - 2);
    }

    public static void copyThis(String toCopy) {
        copyThis(true, toCopy);
    }

    public static void copyThis(boolean showToast, String toCopy) {
        EventListener copyListener = e -> {
            ClipboardEvent clipboardEvent = Js.uncheckedCast(e);
            clipboardEvent.clipboardData.setData("text/plain", toCopy);
            e.preventDefault();
        };
        DomGlobal.document.addEventListener("copy", copyListener);
        docExecCommandCopy();
        DomGlobal.document.removeEventListener("copy", copyListener);

        if (!showToast) {
            return;
        }

        MaterialToast.fireToast("Copied to clipboard");
    }

    private static native void docExecCommandCopy() /*-{
        $doc.execCommand('copy');
    }-*/;
}

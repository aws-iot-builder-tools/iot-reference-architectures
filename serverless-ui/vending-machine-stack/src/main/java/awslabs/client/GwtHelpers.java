package awslabs.client;

import com.google.gwt.user.client.Window;
import elemental2.dom.ClipboardEvent;
import elemental2.dom.DomGlobal;
import elemental2.dom.EventListener;
import gwt.material.design.client.ui.MaterialToast;
import jsinterop.base.Js;

import java.util.logging.Logger;

public class GwtHelpers {
    private static final Logger log = Logger.getLogger("");

    public static void info(String message) {
        log.info(message);
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

    public static String getBaseUrl() {
        String href = Window.Location.getHref();
        String path = Window.Location.getPath();
        return href.substring(0, href.indexOf(path) + path.length());
    }

    private static native void docExecCommandCopy() /*-{
        $doc.execCommand('copy');
    }-*/;
}

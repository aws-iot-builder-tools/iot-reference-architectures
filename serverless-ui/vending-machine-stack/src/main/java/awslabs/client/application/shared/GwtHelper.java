package awslabs.client.application.shared;

import awslabs.client.resources.AppResources;
import awslabs.client.ssm.ClientMessage;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.WidgetCollection;
import elemental2.core.ArrayBuffer;
import elemental2.core.Int8Array;
import elemental2.dom.DomGlobal;
import elemental2.dom.ViewCSS;
import elemental2.dom.WebSocket;
import gwt.material.design.client.ui.MaterialLink;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import jsinterop.base.Js;
import org.jboss.elemento.By;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.jboss.elemento.Elements.body;

public class GwtHelper {
    private static final Logger log = Logger.getLogger("");

    private static final ViewCSS viewCSS = Js.<ViewCSS>cast(DomGlobal.window);

    public static void customTransition(Widget widget, AppResources res, String transitionName) {
        widget.removeStyleName(res.style().notransition());
        widget.addStyleName(transitionName);

        Timer timer = new Timer() {
            @Override
            public void run() {
                widget.addStyleName(res.style().notransition());
                widget.removeStyleName(transitionName);
            }
        };

        timer.schedule(getTransitionDurationMilliseconds(widget));
    }

    private static int getTransitionDurationMilliseconds(Widget widget) {
        return getTransitionDurationMilliseconds(widget, 1000);
    }

    private static int getTransitionDurationMilliseconds(Widget widget, int defaultDurationMilliseconds) {
        return Option.of(widget.getElement())
                .map(Element::getId)
                // Can't do anything if the ID is empty
                .filter(id -> !id.isEmpty())
                // Get a selector for the ID
                .map(By::id)
                // Find all of the elements with the selector
                .map(selector -> body().findAll(selector))
                // Get an iterator
                .map(Iterable::iterator)
                // Make sure it has at least one element
                .filter(Iterator::hasNext)
                // Get the next element
                .map(Iterator::next)
                // Get the computed style
                .map(viewCSS::getComputedStyle)
                // SAFETY! Get computed style can return NULL in certain browsers!
                .flatMap(Option::of)
                // Get the transition duration property
                .map(cssStyleDeclaration -> cssStyleDeclaration.getPropertyValue("transition-duration"))
                // SAFETY! Get property value can return NULL in certain browsers!
                .flatMap(Option::of)
                // Make sure it matches the regex
                .filter(string -> string.matches("^[0-9]+(\\.[0-9]+)?s$"))
                // Pull off the last character ('s')
                .map(string -> string.substring(0, string.length() - 1))
                // Parse it into an integer
                .map(Double::parseDouble)
                // Multiply it by 1000 to make it milliseconds
                .map(seconds -> seconds * 1000)
                // Convert it to an integer
                .map(Double::intValue)
                // If anything fails just use the default duration
                .getOrElse(defaultDurationMilliseconds);
    }

    public static void info(String message) {
        log.info(message);
    }

    // Guidance from: https://stackoverflow.com/a/50459358
    public native static String uuid() /*-{
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,
            function (c) {
                var r = Math.random() * 16 | 0, v = c == 'x' ? r
                    : (r & 0x3 | 0x8);
                return v.toString(16);
            });
    }-*/;

    @NotNull
    public static String hexStringToBinary(String hexString) {
        StringBuilder stringBuilder = new StringBuilder();

        int chunkSize = 2;
        int numberOfChunks = (hexString.length() + chunkSize - 1) / chunkSize;

        IntStream.range(0, numberOfChunks)
                .mapToObj(index -> hexString.substring(index * chunkSize, Math.min((index + 1) * chunkSize, hexString.length())))
                .map(hex -> Integer.parseInt(hex, 16))
                .map(Integer::toBinaryString)
                // Must add left padding with zeroes or we will get strange results
                .map(string -> repeat("0", 8 - string.length()) + string)
                .forEach(stringBuilder::append);

        return stringBuilder.toString();
    }

    private static String repeat(String value, int count) {
        return new String(new char[count]).replace("\0", value);
    }

    public static byte[] hexStringToBytes(String hexString) {
        byte[] byteArray = new BigInteger("1" + hexString, 16).toByteArray();

        // Remove the extra byte at the beginning (from the leading 1)
        return Arrays.copyOfRange(byteArray, 1, byteArray.length);
    }

    public static String bytesToHexString(byte[] bytes, int length) {
        StringBuilder stringBuilder = new StringBuilder();

        int padding = length - bytes.length;

        for (int loop = 0; loop < padding; loop++) {
            stringBuilder.append("00");
        }

        List.ofAll(bytes)
                .map(GwtHelper::singleByteToHexString)
                .forEach(stringBuilder::append);

        return stringBuilder.toString();
    }

    public static String bytesToHexString(long value, int bytes) {
        StringBuilder stringBuilder = new StringBuilder();

        int bitLength = bytes * 8;

        for (int loop = 0; loop < bytes; loop++) {
            int shiftValue = bitLength - ((loop + 1) * 8);
            int currentByte = Math.toIntExact((value >> shiftValue) & 0xFF);
            stringBuilder.append(singleByteToHexString(currentByte));
        }

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

    public static byte[] arrayBufferToByteArray(ArrayBuffer arrayBuffer) {
        return Js.<byte[]>uncheckedCast(new Int8Array(arrayBuffer));
    }

    private static Int8Array byteArrayToInt8Array(byte[] byteArray) {
        return new Int8Array(Js.<double[]>uncheckedCast(byteArray));
    }

    private static void sendBinary(WebSocket webSocket, byte[] data) {
        webSocket.send(byteArrayToInt8Array(data));
    }

    public static void sendBinary(WebSocket webSocket, ClientMessage clientMessage) {
        sendBinary(webSocket, clientMessage.toByteArray());
    }

    public static <T, V extends Iterable<?>> List<T> castListElements(V iterable, Class<T> clazz) {
        return castListElements(List.ofAll(iterable), clazz);
    }

    public static <T> List<T> castListElements(List<Object> list, Class<T> clazz) {
        return list.map(value -> Try.of(() -> (T) value))
                .filter(Try::isSuccess)
                .map(Try::get);
    }
}

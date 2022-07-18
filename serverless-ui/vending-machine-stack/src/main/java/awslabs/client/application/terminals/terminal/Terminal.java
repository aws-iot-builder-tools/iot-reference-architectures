package awslabs.client.application.terminals.terminal;

import com.google.gwt.dom.client.Element;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Terminal {
    public native void open(Element element);

    public native void write(String data);
}

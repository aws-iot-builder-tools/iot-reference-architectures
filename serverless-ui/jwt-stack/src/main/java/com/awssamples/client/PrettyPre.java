package com.awssamples.client;

import gwt.material.design.client.ui.html.Pre;

public class PrettyPre extends Pre {
    public PrettyPre(String text) {
        super(text);
        addStyleName("prettyprint");
    }

    @Override
    protected void onLoad() {
        super.onLoad();

        // When the widget loads, force the styling of pretty print
        prettyPrint();
    }

    private native void prettyPrint() /*-{
        $wnd.prettyPrint();
    }-*/;
}

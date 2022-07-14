package awslabs.client.application.terminals.terminal;

import awslabs.client.application.events.CustomKey;
import awslabs.client.application.events.SsmData;
import awslabs.client.application.events.TerminalClosed;
import awslabs.client.application.shared.MainNavigationHandler;
import awslabs.client.ssm.SsmWebSocket;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialCardContent;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.Lazy;

public class TerminalWidget implements MainNavigationHandler {
    private static final TerminalWidget.Binder binder = GWT.create(TerminalWidget.Binder.class);
    public final String terminalPrefix = "terminal";
    private final SsmWebSocket ssmWebSocket;
    private final EventBus eventBus;
    private final Lazy<String> lazyToken = Lazy.of(this::innerGetToken);
    private final Widget root;
    @UiField
    MaterialCardContent terminalCardContent;
    private final Lazy<Terminal> lazyTerminal = Lazy.of(this::innerGetTerminal);

    public TerminalWidget(EventBus eventBus, SsmWebSocket ssmWebSocket) {
        root = binder.createAndBindUi(this);

        this.eventBus = eventBus;
        this.ssmWebSocket = ssmWebSocket;
        this.navigationHandlerSetup();
        this.getTerminal();
    }

    @Override
    public Widget getWidget() {
        return root;
    }

    @Override
    public String getToken() {
        return lazyToken.get();
    }

    private String innerGetToken() {
        return String.join("-", terminalPrefix, getSessionName());
    }

    @Override
    public void bindEventBus() {
        eventBus.addHandler(SsmData.TYPE, this::onSsmData);
        eventBus.addHandler(CustomKey.TYPE, this::onCustomKey);
    }

    private void onCustomKey(CustomKey.Event event) {
        if (!event.ssmWebSocket.equals(ssmWebSocket)) {
            return;
        }

        if (event.getAsciiValue().isEmpty()) {
            // No values, do nothing
            return;
        }

        event
                // Get the ASCII value(s) for this key
                .getAsciiValue()
                // For each value send it to the session
                .forEach(ssmWebSocket::sendKey);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public Terminal getTerminal() {
        return lazyTerminal.get();
    }

    public String getSessionName() {
        return ssmWebSocket.getSessionName();
    }

    private Terminal innerGetTerminal() {
        terminalCardContent.addKeyUpHandler(event -> eventBus.fireEvent(new CustomKey.Event(ssmWebSocket, event)));

        Terminal terminal = new Terminal();
        terminal.open(terminalCardContent.getElement());
        return terminal;
    }

    private void onSsmData(SsmData.Event event) {
        if (!event.ssmWebSocket.equals(ssmWebSocket)) {
            return;
        }

        getTerminal().write(new String(event.data));
    }

    public void close() {
        ssmWebSocket.close();
    }

    @UiHandler("close")
    public void onCloseClicked(ClickEvent clickEvent) {
        eventBus.fireEvent(new TerminalClosed.Event(this));
    }

    interface Binder extends UiBinder<Widget, TerminalWidget> {
    }
}

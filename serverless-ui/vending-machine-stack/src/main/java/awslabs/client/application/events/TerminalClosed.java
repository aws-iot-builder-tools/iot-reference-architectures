package awslabs.client.application.events;

import awslabs.client.application.terminals.terminal.TerminalWidget;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class TerminalClosed {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onTerminalClosed(Event terminalClosedEvent);
    }

    public static class Event extends GwtEvent<TerminalClosed.Handler> {
        public final TerminalWidget terminalWidget;

        public Event(TerminalWidget terminalWidget) {
            this.terminalWidget = terminalWidget;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onTerminalClosed(this);
        }
    }
}

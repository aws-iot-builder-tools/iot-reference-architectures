package awslabs.client.application.events;

import awslabs.client.ssm.ItemInSequence;
import awslabs.client.ssm.SsmWebSocket;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SsmData {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onSsmData(Event ssmDataEvent);
    }

    public static class Event extends GwtEvent<SsmData.Handler> implements ItemInSequence {
        public final SsmWebSocket ssmWebSocket;
        public final byte[] data;
        public final long sequenceNumber;

        public Event(SsmWebSocket ssmWebSocket, byte[] data, Long sequenceNumber) {
            this.ssmWebSocket = ssmWebSocket;
            this.data = data;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onSsmData(this);
        }

        @Override
        public long getSequenceNumber() {
            return sequenceNumber;
        }
    }
}

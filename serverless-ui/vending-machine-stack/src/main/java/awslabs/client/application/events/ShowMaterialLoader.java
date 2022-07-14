package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShowMaterialLoader {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onShowMaterialLoader(Event showMaterialLoaderEvent);
    }

    public static class Event extends GwtEvent<ShowMaterialLoader.Handler> {
        private final boolean show;

        public Event(boolean show) {
            this.show = show;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onShowMaterialLoader(this);
        }
    }
}

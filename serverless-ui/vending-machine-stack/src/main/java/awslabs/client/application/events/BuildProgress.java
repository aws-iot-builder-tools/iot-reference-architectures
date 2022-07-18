package awslabs.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildProgress {
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onBuildFinished(Event buildProgressEvent);
    }

    public static class Event extends GwtEvent<BuildProgress.Handler> {
        public String buildId;
        public Integer totalSteps;
        public Integer currentStep;
        public String comment;
        public Integer stepProgress;

        public Event(String buildId) {
            this.buildId = buildId;
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        public Event totalSteps(int totalSteps) {
            this.totalSteps = totalSteps;

            return this;
        }

        public Event currentStep(int currentStep) {
            this.currentStep = currentStep;

            return this;
        }

        public Event comment(String comment) {
            this.comment = comment;

            return this;
        }

        public Event stepProgress(int stepProgress) {
            this.stepProgress = stepProgress;

            return this;
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onBuildFinished(this);
        }
    }
}

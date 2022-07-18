package awslabs.client.application.models;

import awslabs.client.application.events.BuildProgress;
import awslabs.client.shared.IotBuild;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.util.logging.Logger;

public class BuildList extends ModelList<IotBuild> {
    @Inject
    EventBus eventBus;

    @Inject
    Logger log;

    @Inject
    public BuildList() {
    }

    public Option<IotBuild> buildFinished(String buildId) {
        Option<IotBuild> buildOption = getModelOption(buildId);

        if (buildOption.isEmpty()) {
            Window.alert("Got notification that a build was finished that we didn't know about");
            return Option.none();
        }

        IotBuild iotBuild = buildOption.get();

        // Remove the old one
        remove(iotBuild);

        // Update the build available flag
        iotBuild.buildAvailable(true);

        // Remove the status info
        iotBuild.stepProgressNullable(null)
                .totalStepsNullable(null)
                .currentStepNullable(null)
                .commentNullable(null);

        // Add it back
        append(iotBuild);

        return Option.of(iotBuild);
    }

    public Option<IotBuild> buildProgress(BuildProgress.Event buildProgressEvent) {
        Option<IotBuild> buildOption = getModelOption(buildProgressEvent.buildId);

        if (buildOption.isEmpty()) {
            log.info("Got notification that a build was in progress that we didn't know about, ignoring");
            return Option.none();
        }

        IotBuild iotBuild = buildOption.get();

        // Remove the old one
        remove(iotBuild);

        // Add the progress info
        if (buildProgressEvent.currentStep < 0) {
            iotBuild.currentStepNullable(null);
        } else {
            iotBuild.currentStepNullable(buildProgressEvent.currentStep);
        }

        if (buildProgressEvent.totalSteps < 0) {
            iotBuild.totalStepsNullable(null);
        } else {
            iotBuild.totalStepsNullable(buildProgressEvent.totalSteps);
        }

        iotBuild.stepProgressNullable(buildProgressEvent.stepProgress);
        iotBuild.commentNullable(buildProgressEvent.comment);

        // Add it back
        append(iotBuild);

        return Option.of(iotBuild);
    }
}

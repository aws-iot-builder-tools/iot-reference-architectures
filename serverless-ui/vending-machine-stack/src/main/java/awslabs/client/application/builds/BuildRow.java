package awslabs.client.application.builds;

import awslabs.client.shared.IotBuild;
import awslabs.client.shared.ModelWithId;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class BuildRow extends Composite implements ModelWithId {
    private static final BuildRow.Binder binder = GWT.create(BuildRow.Binder.class);
    @UiField
    MaterialLabel name;
    @UiField
    MaterialLabel buildDuration;
    @UiField
    MaterialLink downloadLink;
    private IotBuild iotBuild;
    private Date startTime;
    private Date endTime;
    private IBuildsPresenter buildsPresenter;
    BuildRow(IBuildsPresenter buildsPresenter, IotBuild iotBuild) {
        initWidget(binder.createAndBindUi(this));
        this.buildsPresenter = buildsPresenter;
        this.iotBuild = iotBuild;
        startTime = new Date();
        update(iotBuild);
    }

    @UiHandler("downloadLink")
    public void onDownloadLinkClicked(ClickEvent clickEvent) {
        buildsPresenter.downloadImage(iotBuild.name());
    }

    public void finished(IotBuild iotBuild) {
        endTime = new Date();

        String timeDiffString = getTimeDiff(startTime, endTime);
        buildDuration.setText("Build duration: " + timeDiffString);

        update(iotBuild);
    }

    public String getTimeDiff(Date start, Date end) {
        String diff = "";
        long timeDiff = Math.abs(end.getTime() - start.getTime());
        long minutes = MILLISECONDS.toMinutes(timeDiff);
        long seconds = MILLISECONDS.toSeconds(timeDiff) - MINUTES.toSeconds(minutes);
        diff = String.join("",
                String.valueOf(minutes),
                " minute(s) ",
                String.valueOf(seconds),
                " second(s)");
        return diff;
    }

    public IotBuild getBuild() {
        return iotBuild;
    }

    public void update(IotBuild iotBuild) {
        this.iotBuild = iotBuild;

        name.setText(getHeaderText());

        if (iotBuild.buildAvailable()) {
            downloadLink.setEnabled(true);
            downloadLink.setText("Download image");
        } else {
            downloadLink.setEnabled(false);
            downloadLink.setText("The image is not ready yet");
        }
    }

    @NotNull
    private String getHeaderText() {
        String headerText = name();

        headerText += iotBuild.commentOption().map(comment -> " [" + comment + "]").getOrElse("");

        if (iotBuild.currentStepOption().isDefined() && iotBuild.totalStepsOption().isDefined()) {
            headerText += " [" + iotBuild.currentStepOption().get() + "/" + iotBuild.totalStepsOption().get() + "]";
        }

        headerText += iotBuild.stepProgressOption().map(stepProgress -> " [" + stepProgress + "%]").getOrElse("");

        return headerText;
    }

    @Override
    public String name() {
        return iotBuild.name();
    }

    interface Binder extends UiBinder<Widget, BuildRow> {
    }
}

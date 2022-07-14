package awslabs.client.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.user.client.rpc.IsSerializable;
import io.vavr.control.Option;

public class IotBuild implements IsSerializable, ModelWithId {
    private String name;
    private String commentNullable = null;
    private Integer totalStepsNullable = null;
    private Integer currentStepNullable = null;
    private Integer stepProgressNullable = null;
    private boolean buildAvailable;

    public IotBuild() {
    }

    public IotBuild(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public boolean buildAvailable() {
        return buildAvailable;
    }

    public IotBuild name(String name) {
        this.name = name;

        return this;
    }

    public IotBuild commentNullable(String commentNullable) {
        this.commentNullable = commentNullable;

        return this;
    }

    public IotBuild totalStepsNullable(Integer totalStepsNullable) {
        this.totalStepsNullable = totalStepsNullable;

        return this;
    }

    public IotBuild currentStepNullable(Integer currentStepNullable) {
        this.currentStepNullable = currentStepNullable;

        return this;
    }

    public IotBuild stepProgressNullable(Integer stepProgressNullable) {
        this.stepProgressNullable = stepProgressNullable;

        return this;
    }

    public IotBuild buildAvailable(boolean buildAvailable) {
        this.buildAvailable = buildAvailable;

        return this;
    }

    @JsonIgnore
    public Option<String> commentOption() {
        return Option.of(commentNullable);
    }

    @JsonIgnore
    public Option<Integer> totalStepsOption() {
        return Option.of(totalStepsNullable);
    }

    @JsonIgnore
    public Option<Integer> currentStepOption() {
        return Option.of(currentStepNullable);
    }

    @JsonIgnore
    public Option<Integer> stepProgressOption() {
        return Option.of(stepProgressNullable);
    }
}

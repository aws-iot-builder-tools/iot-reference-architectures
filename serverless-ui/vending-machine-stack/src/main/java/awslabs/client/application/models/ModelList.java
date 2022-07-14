package awslabs.client.application.models;

import awslabs.client.shared.ModelWithId;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.util.Objects;

public class ModelList<T extends ModelWithId> {
    private List<T> innerModelList = List.empty();

    public void clear() {
        innerModelList = List.empty();
    }

    public Option<T> addModel(T newModel) {
        if (!innerModelList
                .filter(existingModel -> Objects.equals(existingModel.name(), newModel.name()))
                .isEmpty()) {
            // We already have this model, ignore it
            return Option.none();
        }

        // New model, add it
        innerModelList = innerModelList.append(newModel);

        // Indicate we added a model
        return Option.of(newModel);
    }

    public Option<T> getModelOption(String id) {
        return innerModelList
                .filter(existingBuild -> id.equals(existingBuild.name()))
                .toOption();
    }

    public void append(T t) {
        innerModelList = innerModelList.append(t);
    }

    public void remove(T t) {
        innerModelList = innerModelList.remove(t);
    }

    public int size() {
        return innerModelList.size();
    }
}

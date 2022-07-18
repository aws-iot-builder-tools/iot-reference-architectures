package awslabs.client.application.builds;

import awslabs.client.application.shared.IsWidget;
import awslabs.client.shared.IotBuild;
import io.vavr.control.Option;

public interface IBuildsView extends IsWidget {
    void clear();

    void showBuildsLoading();

    void showBuilds();

    Option<IotBuild> updateBuild(BuildsPresenter buildsPresenter, IotBuild iotBuild);

    Option<IotBuild> addBuild(BuildsPresenter buildsPresenter, IotBuild iotBuild);

    Option<IotBuild> finishBuild(BuildsPresenter buildsPresenter, IotBuild iotBuild);
}

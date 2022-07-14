package awslabs.client.application.systems;

import awslabs.client.application.shared.IsWidget;
import awslabs.client.shared.IotSystem;
import io.vavr.control.Option;

public interface ISystemsView extends IsWidget {
    void showSystemsLoading();

    void showSystems();

    Option<IotSystem> updateSystem(ISystemsPresenter systemsPresenter, IotSystem iotSystem);

    Option<IotSystem> addSystem(ISystemsPresenter systemsPresenter, IotSystem iotSystem);

    void clear();
}

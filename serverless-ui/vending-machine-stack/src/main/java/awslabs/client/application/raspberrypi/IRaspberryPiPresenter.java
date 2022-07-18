package awslabs.client.application.raspberrypi;

import awslabs.client.application.shared.MainNavigationHandler;
import awslabs.client.application.events.BuildRequestedByUser;

public interface IRaspberryPiPresenter extends MainNavigationHandler, BuildRequestedByUser.Handler {
}

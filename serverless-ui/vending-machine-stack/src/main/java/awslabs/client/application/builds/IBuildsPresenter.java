package awslabs.client.application.builds;

import awslabs.client.application.shared.MainNavigationHandler;

public interface IBuildsPresenter extends MainNavigationHandler {
    void downloadImage(String buildId);
}

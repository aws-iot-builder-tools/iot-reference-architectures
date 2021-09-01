package com.awssamples.client.shell;

import com.awssamples.client.IsWidget;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialLink;

public interface IShellView extends IsWidget {
    MaterialLink getDevBoardWidget();

    MaterialContainer getMainContainer();
}

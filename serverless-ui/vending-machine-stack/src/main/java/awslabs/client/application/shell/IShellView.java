package awslabs.client.application.shell;

import awslabs.client.application.shared.IsWidget;
import awslabs.client.application.terminals.terminal.TerminalWidget;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialNavBrand;

public interface IShellView extends IsWidget {
    MaterialLink getDevBoardWidget();

    MaterialNavBrand getNavBrand();

    void updateBuildCount(int count);

    void updateSystemCount(int count);

    MaterialContainer getMainContainer();

    void addTerminalWidget(TerminalWidget terminalWidget);

    void updateTerminalCount();

    void removeTerminalWidget(TerminalWidget terminalWidget);
}

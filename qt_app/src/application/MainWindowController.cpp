#include "MainWindowController.hpp"

MainWindowController::MainWindowController(QObject* parent)
    : QObject(parent)
{
}

void MainWindowController::setSelectedTabIndex(int index) {
    if (m_selectedTabIndex != index) {
        m_selectedTabIndex = index;
        emit selectedTabIndexChanged(index);
    }
}

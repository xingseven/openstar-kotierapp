#ifndef MAINWINDOWCONTROLLER_HPP
#define MAINWINDOWCONTROLLER_HPP

#include <QObject>

// QML 主窗口控制器 —— 处理 QML 与 C++ 之间的通信
// 属性绑定：QML 通过注册的 context property 访问此对象
class MainWindowController : public QObject {
    Q_OBJECT
    Q_PROPERTY(int selectedTabIndex READ selectedTabIndex WRITE setSelectedTabIndex NOTIFY selectedTabIndexChanged)

public:
    explicit MainWindowController(QObject* parent = nullptr);

    int selectedTabIndex() const { return m_selectedTabIndex; }
    void setSelectedTabIndex(int index);

signals:
    void selectedTabIndexChanged(int index);

private:
    int m_selectedTabIndex = 0;
};

#endif // MAINWINDOWCONTROLLER_HPP

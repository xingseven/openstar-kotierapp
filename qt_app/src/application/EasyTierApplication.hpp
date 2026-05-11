#ifndef EASYTIERAPPLICATION_HPP
#define EASYTIERAPPLICATION_HPP

#include <QGuiApplication>
#include "service/SettingsRepository.hpp"

class EasyTierApplication : public QGuiApplication {
    Q_OBJECT
public:
    EasyTierApplication(int& argc, char** argv);
    ~EasyTierApplication();

    SettingsRepository* settingsRepository() const { return m_settingsRepo; }

private:
    SettingsRepository* m_settingsRepo = nullptr;
};

#endif // EASYTIERAPPLICATION_HPP

#pragma once
#include <QObject>
#include <QFileSystemWatcher>
#include <QColor>

class Theme final : public QObject {
    Q_OBJECT
    Q_PROPERTY(QColor accent READ accent NOTIFY accentChanged)
public:
    explicit Theme(QObject *parent = nullptr);
    QColor accent() const { return m_accent; }
signals:
    void accentChanged();
private:
    QColor m_accent = QColor(QStringLiteral("#FFD60A"));
    QString m_path;
    QFileSystemWatcher m_watcher;
    void reload();
};

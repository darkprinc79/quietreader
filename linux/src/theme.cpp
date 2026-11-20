#include "theme.h"
#include <QFile>
#include <QFileInfo>
#include <QRegularExpression>
#include <QStandardPaths>

Theme::Theme(QObject *parent) : QObject(parent) {
    m_path = QStandardPaths::writableLocation(QStandardPaths::HomeLocation) + QStringLiteral("/.local/state/omarchy/current/theme/colors.toml");
    connect(&m_watcher, &QFileSystemWatcher::fileChanged, this, [this] { reload(); });
    connect(&m_watcher, &QFileSystemWatcher::directoryChanged, this, [this] { reload(); });
    reload();
}
void Theme::reload() {
    const QString directory = QFileInfo(m_path).absolutePath();
    if (!m_watcher.directories().contains(directory)) m_watcher.addPath(directory);
    if (QFileInfo::exists(m_path) && !m_watcher.files().contains(m_path)) m_watcher.addPath(m_path);
    QFile file(m_path);
    if (file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        const QString text = QString::fromUtf8(file.readAll());
        const auto match = QRegularExpression(QStringLiteral("(?m)^\\s*accent\\s*=\\s*[\"'](#[0-9A-Fa-f]{6})[\"']")).match(text);
        if (match.hasMatch()) { const QColor color(match.captured(1)); if (color.isValid() && color != m_accent) { m_accent = color; emit accentChanged(); } }
    }
    if (!m_watcher.files().contains(m_path) && QFileInfo::exists(m_path)) m_watcher.addPath(m_path);
}

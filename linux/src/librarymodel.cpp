#include "librarymodel.h"
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QStringDecoder>

LibraryModel::LibraryModel(QObject *parent) : QAbstractListModel(parent) {}
int LibraryModel::rowCount(const QModelIndex &parent) const { return parent.isValid() ? 0 : m_entries.size(); }
QVariant LibraryModel::data(const QModelIndex &index, int role) const {
    if (!index.isValid() || index.row() < 0 || index.row() >= m_entries.size()) return {};
    const Entry &entry = m_entries.at(index.row());
    switch (role) {
    case NameRole: return entry.name;
    case PathRole: return entry.path;
    case DepthRole: return entry.depth;
    case DirectoryRole: return entry.directory;
    case ExpandedRole: return entry.expanded;
    default: return {};
    }
}
QHash<int, QByteArray> LibraryModel::roleNames() const {
    return {{NameRole, "name"}, {PathRole, "path"}, {DepthRole, "depth"}, {DirectoryRole, "directory"}, {ExpandedRole, "expanded"}};
}
bool LibraryModel::isTextFile(const QString &path) {
    QFile file(path);
    if (!file.open(QIODevice::ReadOnly)) return false;

    // A content sample catches text files with uncommon or missing extensions.
    const QByteArray sample = file.read(8192);
    if (sample.isEmpty()) return true;
    if (sample.contains('\0')) {
        const bool utf16 = sample.startsWith("\xff\xfe") || sample.startsWith("\xfe\xff");
        if (!utf16) return false;
    }

    const bool utf16le = sample.startsWith("\xff\xfe");
    const bool utf16be = sample.startsWith("\xfe\xff");
    QStringDecoder decoder(utf16le ? QStringDecoder::Utf16LE : utf16be ? QStringDecoder::Utf16BE : QStringDecoder::Utf8);
    QString decoded = decoder.decode(sample);
    if (decoder.hasError()) {
        if (utf16le || utf16be) return false;
        decoded = QString::fromLatin1(sample);
    }

    int controls = 0;
    for (const QChar ch : decoded) {
        if (ch.isNull()) return false;
        if (ch.category() == QChar::Other_Control && ch != QLatin1Char('\n') && ch != QLatin1Char('\r') && ch != QLatin1Char('\t') && ch != QChar(0x0c)) ++controls;
    }
    return decoded.isEmpty() || controls * 100 <= decoded.size();
}
void LibraryModel::setRoot(const QString &path) {
    QString requested = path.trimmed();
    if (requested == QStringLiteral("~")) requested = QDir::homePath();
    else if (requested.startsWith(QStringLiteral("~/"))) requested = QDir::homePath() + requested.mid(1);
    const QString canonical = QDir(requested).canonicalPath();
    if (canonical.isEmpty() || !QFileInfo(canonical).isDir()) {
        m_error = tr("Folder not found or not accessible: %1").arg(path); emit errorChanged(); return;
    }
    m_root = canonical; m_expanded.clear(); m_expanded.insert(canonical); m_error.clear();
    emit errorChanged(); emit rootPathChanged(); rebuild();
}
void LibraryModel::setRootUrl(const QUrl &url) {
    if (!url.isLocalFile()) {
        m_error = tr("The selected folder is not a local folder: %1").arg(url.toString());
        emit errorChanged();
        return;
    }
    setRoot(url.toLocalFile());
}
void LibraryModel::toggle(const QString &path) {
    if (m_expanded.contains(path)) m_expanded.remove(path); else m_expanded.insert(path);
    rebuild();
}
void LibraryModel::appendDirectory(const QString &path, int depth) {
    const QFileInfo info(path);
    const bool open = m_expanded.contains(path);
    m_entries.append({info.fileName().isEmpty() ? path : info.fileName(), path, depth, true, open});
    if (!open) return;
    QDir dir(path);
    const QFileInfoList children = dir.entryInfoList(QDir::Dirs | QDir::Files | QDir::NoDotAndDotDot | QDir::Readable, QDir::DirsFirst | QDir::Name | QDir::IgnoreCase);
    for (const QFileInfo &child : children) {
        if (child.isSymLink()) continue;
        if (child.isDir()) appendDirectory(child.absoluteFilePath(), depth + 1);
        else if (isTextFile(child.absoluteFilePath())) m_entries.append({child.fileName(), child.absoluteFilePath(), depth + 1, false, false});
    }
}
void LibraryModel::rebuild() {
    beginResetModel(); m_entries.clear(); if (!m_root.isEmpty()) appendDirectory(m_root, 0); endResetModel();
}
void LibraryModel::openDocument(const QString &path) {
    QString requested = path.trimmed();
    if (requested.startsWith(QStringLiteral("~/"))) requested = QDir::homePath() + requested.mid(1);
    QFile file(requested);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        m_error = tr("Could not open file: %1").arg(path); emit errorChanged(); return;
    }
    const QByteArray bytes = file.readAll();
    QStringDecoder::Encoding encoding = QStringDecoder::Utf8;
    qsizetype bomSize = 0;
    if (bytes.startsWith("\xff\xfe")) { encoding = QStringDecoder::Utf16LE; bomSize = 2; }
    else if (bytes.startsWith("\xfe\xff")) { encoding = QStringDecoder::Utf16BE; bomSize = 2; }
    else if (bytes.startsWith("\xef\xbb\xbf")) bomSize = 3;
    QStringDecoder decoder(encoding);
    m_text = decoder.decode(bytes.mid(bomSize));
    if (decoder.hasError()) {
        if (encoding == QStringDecoder::Utf8) m_text = QString::fromLatin1(bytes);
        else { m_error = tr("This file is not valid UTF-16 text."); emit errorChanged(); return; }
    }
    m_title = QFileInfo(requested).fileName(); m_error.clear();
    emit documentChanged(); emit errorChanged();
}

#pragma once

#include <QAbstractListModel>
#include <QSet>
#include <QStringList>

class LibraryModel final : public QAbstractListModel {
    Q_OBJECT
    Q_PROPERTY(QString rootPath READ rootPath NOTIFY rootPathChanged)
    Q_PROPERTY(QString currentTitle READ currentTitle NOTIFY documentChanged)
    Q_PROPERTY(QString documentText READ documentText NOTIFY documentChanged)
    Q_PROPERTY(QString error READ error NOTIFY errorChanged)
public:
    enum Role { NameRole = Qt::UserRole + 1, PathRole, DepthRole, DirectoryRole, ExpandedRole };
    explicit LibraryModel(QObject *parent = nullptr);
    int rowCount(const QModelIndex &parent = {}) const override;
    QVariant data(const QModelIndex &index, int role) const override;
    QHash<int, QByteArray> roleNames() const override;
    QString rootPath() const { return m_root; }
    QString currentTitle() const { return m_title; }
    QString documentText() const { return m_text; }
    QString error() const { return m_error; }
    Q_INVOKABLE void setRoot(const QString &path);
    Q_INVOKABLE void toggle(const QString &path);
    Q_INVOKABLE void openDocument(const QString &path);
signals:
    void rootPathChanged();
    void documentChanged();
    void errorChanged();
private:
    struct Entry { QString name, path; int depth; bool directory, expanded; };
    QList<Entry> m_entries;
    QSet<QString> m_expanded;
    QString m_root, m_title, m_text, m_error;
    void rebuild();
    void appendDirectory(const QString &path, int depth);
    static bool isTextFile(const QString &path);
};

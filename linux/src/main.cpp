#include <QGuiApplication>
#include <QFileInfo>
#include <QDir>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include "librarymodel.h"
#include "theme.h"

int main(int argc, char *argv[]) {
    QGuiApplication app(argc, argv);
    app.setApplicationName(QStringLiteral("Quiet Reader"));
    app.setDesktopFileName(QStringLiteral("quietreader"));
    LibraryModel library;
    library.setRoot(QDir::homePath());
    Theme theme;
    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty(QStringLiteral("library"), &library);
    engine.rootContext()->setContextProperty(QStringLiteral("theme"), &theme);
    engine.load(QUrl(QStringLiteral("qrc:/Main.qml")));
    if (engine.rootObjects().isEmpty()) return 1;
    const QStringList arguments = app.arguments();
    if (arguments.size() > 1) {
        const QString path = arguments.at(1);
        if (QFileInfo(path).isDir()) library.setRoot(path);
        else library.openDocument(path);
    }
    return app.exec();
}

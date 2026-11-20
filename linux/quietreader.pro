QT += core gui qml quick quickcontrols2
CONFIG += c++17
TEMPLATE = app
TARGET = quietreader
SOURCES += src/main.cpp src/librarymodel.cpp src/theme.cpp
HEADERS += src/librarymodel.h src/theme.h
RESOURCES += src/resources.qrc
QMAKE_CXXFLAGS += -Wall -Wextra

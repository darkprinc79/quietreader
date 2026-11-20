import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts
import QtQuick.Dialogs

ApplicationWindow {
    id: window
    visible: true
    width: 1100
    height: 720
    minimumWidth: 680
    minimumHeight: 420
    title: library.currentTitle.length ? library.currentTitle + " — Quiet Reader" : qsTr("Quiet Reader")
    color: "#000000"
    Material.theme: Material.Dark
    Material.accent: theme.accent

    property int readerSize: 20
    property color readerColor: "#bebebe"

    FolderDialog {
        id: folderDialog
        title: qsTr("Choose a library folder")
        currentFolder: StandardPaths.standardLocations(StandardPaths.HomeLocation)[0]
        onAccepted: library.setRootUrl(selectedFolder)
    }
    Dialog {
        id: fileDialog
        objectName: "filePathDialog"
        title: qsTr("Open a text file")
        modal: true
        anchors.centerIn: parent
        width: 500
        standardButtons: Dialog.Open | Dialog.Cancel
        onAccepted: library.openDocument(filePath.text)
        contentItem: ColumnLayout {
            implicitWidth: 460
            Label { text: qsTr("File path") }
            TextField { id: filePath; objectName: "filePathField"; Layout.fillWidth: true; placeholderText: qsTr("For example: ~/Documents/book.txt"); onAccepted: fileDialog.accept() }
            Label { text: qsTr("You can also open text files by clicking them in the library tree."); color: "#999999"; wrapMode: Text.WordWrap; Layout.fillWidth: true }
        }
    }

    RowLayout {
        anchors.fill: parent
        spacing: 0

        Rectangle {
            Layout.preferredWidth: Math.min(300, window.width * 0.34)
            Layout.minimumWidth: 210
            Layout.fillHeight: true
            color: "#101010"
            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 10
                spacing: 8
                RowLayout {
                    Layout.fillWidth: true
                    Label { text: qsTr("Library"); font.bold: true; font.pixelSize: 18; Layout.fillWidth: true }
                    ToolButton { text: qsTr("Choose folder"); Accessible.name: qsTr("Choose library folder"); onClicked: folderDialog.open() }
                }
                Label {
                    Layout.fillWidth: true
                    text: library.rootPath.length ? library.rootPath : qsTr("Choose a folder to begin")
                    color: "#888888"; elide: Text.ElideMiddle; font.pixelSize: 11
                }
                ListView {
                    id: tree
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    clip: true
                    model: library
                    delegate: ItemDelegate {
                        required property string name
                        required property string path
                        required property int depth
                        required property bool directory
                        required property bool expanded
                        width: ListView.view.width
                        height: 38
                        leftPadding: 6 + depth * 14
                        text: (directory ? (expanded ? "▾  " : "▸  ") : "   ") + name
                        font.pixelSize: 14
                        highlighted: false
                        onClicked: directory ? library.toggle(path) : library.openDocument(path)
                    }
                    ScrollBar.vertical: ScrollBar {}
                }
                Button { Layout.fillWidth: true; text: qsTr("Open file…"); onClicked: fileDialog.open() }
            }
        }
        Rectangle { Layout.fillHeight: true; Layout.preferredWidth: 1; color: "#383838" }
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: 0
            RowLayout {
                Layout.fillWidth: true
                Layout.margins: 12
                Label { text: library.currentTitle.length ? library.currentTitle : qsTr("Quiet Reader"); font.pixelSize: 18; font.bold: true; elide: Text.ElideRight; Layout.fillWidth: true }
                ToolButton { text: "Aa"; Accessible.name: qsTr("Reading appearance"); onClicked: appearance.open() }
            }
            Rectangle { Layout.fillWidth: true; height: 1; color: "#252525" }
            ScrollView {
                id: readerPane
                Layout.fillWidth: true
                Layout.fillHeight: true
                clip: true
                TextArea {
                    id: readerText
                    width: readerPane.availableWidth
                    height: Math.max(readerPane.availableHeight, contentHeight + topPadding + bottomPadding)
                    text: library.currentTitle.length ? library.documentText : qsTr("Choose a text file from the library to start reading.")
                    readOnly: true
                    selectByMouse: true
                    wrapMode: TextEdit.Wrap
                    color: readerColor
                    font.pixelSize: readerSize
                    font.family: "sans-serif"
                    padding: 28
                    background: null
                    selectionColor: theme.accent
                    selectedTextColor: "#000000"
                }
            }
            Label { visible: library.error.length > 0; text: library.error; color: "#ff8a80"; Layout.margins: 8 }
        }
    }

    Dialog {
        id: appearance
        title: qsTr("Reading appearance")
        modal: true
        anchors.centerIn: parent
        width: 430
        standardButtons: Dialog.Close
        contentItem: ColumnLayout {
            implicitWidth: 390
            Label { text: qsTr("Text size: %1 px").arg(readerSize) }
            Slider { from: 14; to: 42; stepSize: 1; value: readerSize; onMoved: readerSize = Math.round(value); Layout.fillWidth: true }
            Label { text: qsTr("Text color") }
            RowLayout {
                Repeater {
                    model: [{name: qsTr("Dim"), color: "#bebebe"}, {name: qsTr("White"), color: "#ffffff"}, {name: qsTr("Warm"), color: "#eedcb9"}, {name: qsTr("Green"), color: "#b9d7be"}]
                    delegate: Button { required property var modelData; text: modelData.name; onClicked: readerColor = modelData.color }
                }
            }
        }
    }

    Shortcut { sequences: [StandardKey.Open]; context: Qt.ApplicationShortcut; onActivated: fileDialog.open() }
    Shortcut { sequence: "Ctrl+Shift+O"; context: Qt.ApplicationShortcut; onActivated: folderDialog.open() }
    Shortcut { sequence: "Ctrl+Plus"; context: Qt.ApplicationShortcut; onActivated: readerSize = Math.min(42, readerSize + 1) }
    Shortcut { sequence: "Ctrl+Minus"; context: Qt.ApplicationShortcut; onActivated: readerSize = Math.max(14, readerSize - 1) }
    Shortcut { sequence: "Ctrl+Q"; context: Qt.ApplicationShortcut; onActivated: Qt.quit() }
}

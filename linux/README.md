# Quiet Reader for Linux

Quiet Reader is a Qt Quick desktop app for reading plain text. The left pane shows an expandable library tree rooted at your home folder, and the right pane displays text files detected from their contents, regardless of filename extension. Use the plus button to enter another folder path, or click a file in the tree. The Open file button accepts a file path directly. UTF-8 and UTF-16 text are supported. Files are opened read-only.

## Build and run

Requires Qt 6 with Quick Controls and Quick Dialogs, qmake6, make, and a C++17 compiler.

    ./bin/build
    ./build/quietreader

Pass a path to open a text file on startup:

    ./build/quietreader ~/Documents/book.txt

The default reader uses dim gray text on black. The `Aa` menu changes text size and color. On Omarchy, the Material accent follows the active theme.

## Keyboard shortcuts

| Shortcut | Action |
| --- | --- |
| Ctrl+O | Open a text file |
| Ctrl+Shift+O | Choose the library folder |
| Ctrl+Plus / Ctrl+Minus | Increase or decrease text size |
| Ctrl+Q | Quit |

On Arch Linux, `./bin/package` builds a package with `makepkg`; installing it is left to the user.

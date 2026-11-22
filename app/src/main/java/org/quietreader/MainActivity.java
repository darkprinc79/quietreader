package org.quietreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** A small, native Android 5+ text reader with a directory tree sidebar. */
public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 41;
    private static final int BG = Color.rgb(0, 0, 0);
    private static final int DEFAULT_TEXT = Color.rgb(190, 190, 190);
    private static final String PREFS = "reader";

    private LinearLayout tree, sidebar;
    private TextView page, title, folderLabel;
    private Button upButton;
    private ScrollView readerScroll;
    private Uri rootUri;
    private Uri currentDirectoryUri;
    private final List<Uri> directoryStack = new ArrayList<>();
    private int textColor, textSize;
    private boolean compactSidebar;
    private int pad;
    private List<Entry> visibleEntries = new ArrayList<>();
    private int displayedEntryCount;
    private int refreshGeneration;
    private static final int ROWS_PER_PAGE = 200;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        pad = (int) (16 * getResources().getDisplayMetrics().density);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        textColor = getPreferences(0).getInt("color", DEFAULT_TEXT);
        textSize = getPreferences(0).getInt("size", 20);
        String saved = getPreferences(0).getString("folder", null);
        if (saved != null) {
            rootUri = Uri.parse(saved);
            currentDirectoryUri = rootUri;
            directoryStack.add(rootUri);
        }
        buildUi();
        showWelcome();
        if (rootUri != null) folderLabel.setText("Last folder saved — tap Open to load");
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.HORIZONTAL); root.setBackgroundColor(BG);
        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        compactSidebar = screenWidthDp < 600;
        int sidebarWidthDp = compactSidebar ? Math.min(160, Math.max(136, (int) (screenWidthDp * 0.38f))) : 280;
        int sidebarPadding = compactSidebar ? dp(4) : pad / 2;
        sidebar = new LinearLayout(this); sidebar.setOrientation(LinearLayout.VERTICAL); sidebar.setPadding(sidebarPadding, sidebarPadding, sidebarPadding, sidebarPadding); sidebar.setBackgroundColor(Color.rgb(15,15,15));
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        Button choose = button(compactSidebar ? "Open" : "Open folder");
        choose.setContentDescription("Choose library folder");
        choose.setOnClickListener(v -> openFolderMenu());
        upButton = button("Up");
        upButton.setOnClickListener(v -> navigateUp());
        top.addView(choose, new LinearLayout.LayoutParams(0, dp(42), 1));
        top.addView(upButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        sidebar.addView(top);
        folderLabel = label("Choose folder", compactSidebar ? 12 : 14, Color.LTGRAY);
        folderLabel.setSingleLine(true);
        sidebar.addView(folderLabel, new LinearLayout.LayoutParams(-1, dp(32)));
        ScrollView treeScroll = new ScrollView(this); tree = new LinearLayout(this); tree.setOrientation(LinearLayout.VERTICAL); treeScroll.addView(tree);
        sidebar.addView(treeScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        View divider = new View(this); divider.setBackgroundColor(Color.rgb(55,55,55));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setBackgroundColor(BG);
        LinearLayout toolbar = new LinearLayout(this); toolbar.setOrientation(LinearLayout.HORIZONTAL); toolbar.setGravity(Gravity.CENTER_VERTICAL); toolbar.setPadding(pad, pad/2, pad, pad/2);
        title = label("Quiet Reader", 18, Color.WHITE); title.setTypeface(null, Typeface.BOLD);
        Button appearance = button("Aa"); appearance.setOnClickListener(v -> appearanceDialog());
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1)); toolbar.addView(appearance);
        readerScroll = new ScrollView(this); readerScroll.setFillViewport(true);
        page = label("Choose a folder, then select a text file from the library.", textSize, textColor); page.setGravity(Gravity.TOP | Gravity.START); page.setPadding(pad * 2, pad, pad * 2, pad * 2); page.setTextIsSelectable(true); page.setTextSize(textSize);
        readerScroll.addView(page);
        content.addView(toolbar); content.addView(readerScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(sidebar, new LinearLayout.LayoutParams(dp(sidebarWidthDp), -1)); root.addView(divider, new LinearLayout.LayoutParams(dp(1), -1)); root.addView(content, new LinearLayout.LayoutParams(0, -1, 1));
        setContentView(root);
    }

    private void showWelcome() { title.setText("Quiet Reader"); page.setText("Choose a folder, then select a text file from the library.\n\nText files (.txt) and files without an extension are supported."); }
    private void openFolderMenu() {
        if (rootUri == null) { pickFolder(); return; }
        new AlertDialog.Builder(this).setTitle("Open a folder")
            .setItems(new String[]{"Resume last folder", "Choose another folder"}, (dialog, which) -> {
                if (which == 0) {
                    currentDirectoryUri = rootUri;
                    directoryStack.clear();
                    directoryStack.add(rootUri);
                    refreshTree();
                } else pickFolder();
            }).show();
    }
    private void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_FOLDER);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            rootUri = data.getData();
            try { getContentResolver().takePersistableUriPermission(rootUri, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) { }
            getPreferences(0).edit().putString("folder", rootUri.toString()).apply();
            directoryStack.clear();
            directoryStack.add(rootUri);
            currentDirectoryUri = rootUri;
            refreshTree();
        }
    }
    private void refreshTree() {
        if (rootUri == null) return;
        if (currentDirectoryUri == null) currentDirectoryUri = rootUri;
        final int generation = ++refreshGeneration;
        final Uri directory = currentDirectoryUri;
        tree.removeAllViews();
        folderLabel.setText("Loading folder…");
        upButton.setEnabled(directoryStack.size() > 1);
        visibleEntries.clear();
        displayedEntryCount = 0;
        TextView loading = label("Loading folder…", compactSidebar ? 12 : 14, Color.LTGRAY);
        tree.addView(loading);
        new AsyncTask<Void, Void, List<Entry>>() {
            @Override protected List<Entry> doInBackground(Void... ignored) {
                try {
                    List<Entry> entries = list(directory);
                    Collections.sort(entries, Comparator.comparing((Entry e) -> !e.directory).thenComparing(e -> e.name.toLowerCase()));
                    return entries;
                } catch (Exception e) { return null; }
            }
            @Override protected void onPostExecute(List<Entry> entries) {
                if (generation != refreshGeneration || isFinishing()) return;
                tree.removeAllViews();
                if (entries == null) {
                    folderLabel.setText("Could not load folder");
                    tree.addView(label("Folder could not be read. Choose it again or try a smaller folder.", compactSidebar ? 12 : 14, Color.LTGRAY));
                    return;
                }
                folderLabel.setText(displayName(directory));
                visibleEntries = entries;
                showNextPage();
                if (visibleEntries.isEmpty()) tree.addView(label("No readable text files or folders.", compactSidebar ? 12 : 14, Color.LTGRAY));
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private void showNextPage() {
        int end = Math.min(displayedEntryCount + ROWS_PER_PAGE, visibleEntries.size());
        for (int i = displayedEntryCount; i < end; i++) {
            Entry entry = visibleEntries.get(i);
            if (entry.directory) addDirectory(entry);
            else addFile(entry);
        }
        displayedEntryCount = end;
        if (displayedEntryCount < visibleEntries.size()) {
            Button more = button("Show next " + Math.min(ROWS_PER_PAGE, visibleEntries.size() - displayedEntryCount) + " items");
            more.setOnClickListener(v -> { tree.removeView(more); showNextPage(); });
            tree.addView(more);
        }
    }
    private void navigateUp() {
        if (directoryStack.size() <= 1) return;
        directoryStack.remove(directoryStack.size() - 1);
        currentDirectoryUri = directoryStack.get(directoryStack.size() - 1);
        refreshTree();
    }
    private void addDirectory(Entry entry) {
        TextView item = label("> " + entry.name, compactSidebar ? 12 : 14, Color.rgb(205,205,205));
        item.setSingleLine(true); item.setPadding(dp(6), 0, dp(4), 0); item.setGravity(Gravity.CENTER_VERTICAL); item.setMinHeight(dp(compactSidebar ? 34 : 40));
        item.setOnClickListener(v -> {
            currentDirectoryUri = entry.uri;
            directoryStack.add(entry.uri);
            refreshTree();
        });
        tree.addView(item);
    }
    private void addFile(Entry entry) {
        TextView item = label(entry.name, compactSidebar ? 12 : 14, Color.rgb(170,170,170));
        item.setSingleLine(true); item.setPadding(dp(10), 0, dp(4), 0); item.setGravity(Gravity.CENTER_VERTICAL); item.setMinHeight(dp(compactSidebar ? 32 : 38));
        item.setOnClickListener(v -> openFile(entry)); tree.addView(item);
    }
    private void openFile(Entry e) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(e.uri), "UTF-8"));
            StringBuilder b = new StringBuilder(); String line; while ((line = reader.readLine()) != null) b.append(line).append('\n'); reader.close();
            title.setText(e.name); page.setText(b.toString()); page.setTextSize(textSize); page.setTextColor(textColor); readerScroll.scrollTo(0,0);
        } catch (Exception ex) { Toast.makeText(this, "Could not open this file", Toast.LENGTH_SHORT).show(); }
    }
    private List<Entry> list(Uri directory) throws Exception {
        List<Entry> out = new ArrayList<>();
        String id = directory.equals(rootUri) ? DocumentsContract.getTreeDocumentId(rootUri) : DocumentsContract.getDocumentId(directory);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, id);
        try (android.database.Cursor c = getContentResolver().query(children, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
            if (c != null) { while (c.moveToNext()) {
                String childId = c.getString(0), name = c.getString(1), mime = c.getString(2);
                boolean isDirectory = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                String displayName = name == null ? "(unnamed)" : name;
                if (isDirectory || isReadableText(displayName)) {
                    Uri childUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, childId);
                    out.add(new Entry(childUri, displayName, isDirectory));
                }
            } }
        }
        return out;
    }
    private String displayName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null,null,null)) { if (c != null && c.moveToFirst()) { String n=c.getString(0); return n; } } catch(Exception ignored) { }
        return "Selected folder";
    }
    private boolean isReadableText(String name) { int dot = name.lastIndexOf('.'); return dot < 0 || name.substring(dot).equalsIgnoreCase(".txt"); }
    private void appearanceDialog() {
        LinearLayout box = new LinearLayout(this); box.setPadding(pad, 0, pad, 0); box.setOrientation(LinearLayout.VERTICAL);
        TextView sizeLabel = label("Text size: " + textSize + " sp", 16, Color.WHITE); box.addView(sizeLabel);
        android.widget.SeekBar size = new android.widget.SeekBar(this); size.setMax(32); size.setProgress(textSize - 12); box.addView(size);
        size.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() { public void onStartTrackingTouch(android.widget.SeekBar b){} public void onStopTrackingTouch(android.widget.SeekBar b){} public void onProgressChanged(android.widget.SeekBar b,int p,boolean u){ textSize=12+p; sizeLabel.setText("Text size: "+textSize+" sp"); page.setTextSize(textSize); } });
        TextView colorLabel = label("Text color", 16, Color.WHITE); colorLabel.setPadding(0,pad/2,0,0); box.addView(colorLabel);
        LinearLayout colors = new LinearLayout(this); colors.setOrientation(LinearLayout.HORIZONTAL);
        addColor(colors, "Dim", Color.rgb(190,190,190)); addColor(colors, "White", Color.WHITE); addColor(colors, "Warm", Color.rgb(238,220,185)); addColor(colors, "Green", Color.rgb(185,215,190));
        LinearLayout extraColors = new LinearLayout(this); extraColors.setOrientation(LinearLayout.HORIZONTAL);
        addColor(extraColors, "Matrix", Color.rgb(0,255,65)); addColor(extraColors, "Blue", Color.rgb(80,150,255)); addColor(extraColors, "Turquoise", Color.rgb(64,224,208));
        box.addView(colors); box.addView(extraColors);
        new AlertDialog.Builder(this).setTitle("Reading appearance").setView(box).setPositiveButton("Done", (d,w) -> getPreferences(0).edit().putInt("size",textSize).putInt("color",textColor).apply()).show();
    }
    private void addColor(LinearLayout row, String name, int color) { Button b = button(name); b.setTextColor(color); b.setOnClickListener(v -> { textColor=color; page.setTextColor(color); }); row.addView(b, new LinearLayout.LayoutParams(0,dp(48),1)); }
    private TextView label(String s, int size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(13); return b; }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
    private static class Entry { final Uri uri; final String name; final boolean directory; Entry(Uri u,String n,boolean d){uri=u;name=n;directory=d;} }
}

package com.keylesspalace.tusky.util;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.widget.EditText;
import me.thanel.markdownedit.SelectionUtils;

public class BBCodeEdit {
    private BBCodeEdit() { /* cannot be instantiated */ }

    public static void addBold(@NonNull Editable text) {
        HTMLEdit.surroundSelectionWith(text, "[b]", "[/b]");
    }

    public static void addBold(@NonNull EditText editText) {
        addBold(editText.getText());
    }

    public static void addItalic(@NonNull Editable text) {
        HTMLEdit.surroundSelectionWith(text, "[i]", "[/i]");
    }

    public static void addItalic(@NonNull EditText editText) {
        addItalic(editText.getText());
    }

    public static void addStrikeThrough(@NonNull Editable text) {
        HTMLEdit.surroundSelectionWith(text, "[s]", "[/s]");
    }

    public static void addStrikeThrough(@NonNull EditText editText) {
        addStrikeThrough(editText.getText());
    }

    public static void addLink(@NonNull Editable text) {
        if (!SelectionUtils.hasSelection(text)) {
            SelectionUtils.selectWordAroundCursor(text);
        }
        String selectedText = SelectionUtils.getSelectedText(text).toString().trim();

        int selectionStart = SelectionUtils.getSelectionStart(text);

        String begin = "[url=url]";
        String end = "[/url]";
        String result = begin + selectedText + end;
        SelectionUtils.replaceSelectedText(text, result);

        if (selectedText.length() == 0) {
            Selection.setSelection(text, selectionStart + begin.length());
        } else {
            selectionStart = selectionStart + 5; // [url=".length()
            Selection.setSelection(text, selectionStart, selectionStart + 3);
        }
    }

    public static void addLink(@NonNull EditText editText) {
        addLink(editText.getText());
    }
    
    /**
     * Inserts a markdown code block to the specified EditText at the currently selected position.
     *
     * @param text The {@link Editable} view to which to add markdown code block.
     */
    public static void addCode(@NonNull Editable text) {
        HTMLEdit.surroundSelectionWith(text, "[code]", "[/code]");
    }

    /**
     * Inserts a markdown code block to the specified EditText at the currently selected position.
     *
     * @param editText The {@link EditText} view to which to add markdown code block.
     */
    public static void addCode(@NonNull EditText editText) {
        addCode(editText.getText());
    }
}
 

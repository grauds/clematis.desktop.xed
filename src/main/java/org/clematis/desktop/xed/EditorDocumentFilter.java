package org.clematis.desktop.xed;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class EditorDocumentFilter extends DocumentFilter {
    private final XmlDocumentEngine engine;
    private final Runnable uiRefreshCallback;
    private final java.util.function.Consumer<Integer> caretUpdateCallback;
    private boolean enabled = true;

    public EditorDocumentFilter(XmlDocumentEngine engine, Runnable uiRefreshCallback, java.util.function.Consumer<Integer> caretUpdateCallback) {
        this.engine = engine;
        this.uiRefreshCallback = uiRefreshCallback;
        this.caretUpdateCallback = caretUpdateCallback;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (!enabled) {
            super.insertString(fb, offset, string, attr);
            return;
        }
        engine.insertText(offset, string);
        caretUpdateCallback.accept(offset + string.length());
        uiRefreshCallback.run();
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (!enabled) {
            super.replace(fb, offset, length, text, attrs);
            return;
        }
        if (length > 0) {
            engine.deleteTextRange(offset, length);
        }
        if (text != null && !text.isEmpty()) {
            engine.insertText(offset, text);
        }
        caretUpdateCallback.accept(offset + (text != null ? text.length() : 0));
        uiRefreshCallback.run();
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        if (!enabled) {
            super.remove(fb, offset, length);
            return;
        }
        engine.deleteTextRange(offset, length);
        caretUpdateCallback.accept(offset);
        uiRefreshCallback.run();
    }
}

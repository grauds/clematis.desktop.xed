package org.clematis.desktop.xed;
/* ----------------------------------------------------------------------------
   Java Workspace
   Copyright (C) 2026 Anton Troshin
   This file is part of Java Workspace.
   This application is free software; you can redistribute it and/or
   modify it under the terms of the GNU Library General Public
   License as published by the Free Software Foundation; either
   version 2 of the License, or (at your option) any later version.
   This application is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Library General Public License for more details.
   You should have received a copy of the GNU Library General Public
   License along with this application; if not, write to the Free
   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
   The author may be contacted at:
   anton.troshin@gmail.com
  ----------------------------------------------------------------------------
 */

import java.util.function.Consumer;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import lombok.Getter;
import lombok.Setter;

/**
 * A custom document filter for editing XML documents with support for real-time
 * UI updates and caret position adjustment. This class extends {@link DocumentFilter}
 * and delegates text manipulation operations to an {@link XmlDocumentEngine}.
 * <p>
 * This filter also provides hooks to trigger UI refreshes and caret positioning
 * changes whenever text operations occur on the document. The behavior of the
 * filter can be toggled on or off via the {@code enabled} property.
 */
public class EditorDocumentFilter extends DocumentFilter {
    private final XmlDocumentEngine engine;
    private final Runnable uiRefreshCallback;
    private final Consumer<Integer> caretUpdateCallback;
    @Getter
    @Setter
    private boolean enabled = true;

    public EditorDocumentFilter(XmlDocumentEngine engine,
                                Runnable uiRefreshCallback,
                                Consumer<Integer> caretUpdateCallback
    ) {
        this.engine = engine;
        this.uiRefreshCallback = uiRefreshCallback;
        this.caretUpdateCallback = caretUpdateCallback;
    }

    @Override
    public void insertString(FilterBypass fb,
                             int offset,
                             String string,
                             AttributeSet attr
    ) throws BadLocationException {

        if (!enabled) {
            super.insertString(fb, offset, string, attr);
            return;
        }
        engine.insertText(offset, string);
        caretUpdateCallback.accept(offset + string.length());
        uiRefreshCallback.run();
    }

    @Override
    public void replace(FilterBypass fb,
                        int offset,
                        int length,
                        String text,
                        AttributeSet attrs
    ) throws BadLocationException {

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

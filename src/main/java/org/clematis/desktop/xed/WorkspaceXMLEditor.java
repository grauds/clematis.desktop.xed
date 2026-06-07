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
import java.io.IOException;

import javax.swing.SwingUtilities;

import jworkspace.ui.api.views.DefaultCompoundView;

public class WorkspaceXMLEditor extends DefaultCompoundView {


    @SuppressWarnings("checkstyle:MagicNumber")
    static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("XML Editor");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            StyledJavaXmlEditor editor = new StyledJavaXmlEditor();
            frame.add(editor);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    @Override
    public void load() throws IOException {

    }

    @Override
    public void save() throws IOException {

    }

    @Override
    public void reset() {

    }
}

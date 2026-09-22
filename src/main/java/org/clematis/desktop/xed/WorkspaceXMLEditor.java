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
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import com.hyperrealm.kiwi.io.ConfigFile;
import com.hyperrealm.kiwi.util.ResourceLoader;
import com.hyperrealm.kiwi.util.ResourceManager;

import jworkspace.config.ServiceLocator;
import jworkspace.runtime.plugin.WorkspacePluginContext;
import jworkspace.ui.api.IView;
import jworkspace.ui.api.cpanel.CButton;
import jworkspace.ui.api.views.DefaultCompoundView;
import lombok.Getter;

public class WorkspaceXMLEditor extends DefaultCompoundView {
    /**
     * Editor properties
     */
    public static final String CK_FONT_FACE = "font.face",
        CK_FONT_SIZE = "font.size",
        CK_FONT_STYLE = "font.style";
    /**
     * Configuration
     */
    private final ConfigFile config;

    private final StyledJavaXmlEditor editor = new StyledJavaXmlEditor();

    @Getter
    private final WorkspacePluginContext pluginContext;

    public WorkspaceXMLEditor(WorkspacePluginContext pluginContext) {
        super();
        this.pluginContext = pluginContext;
        this.config = new ConfigFile(
            this.pluginContext.getUserDir().resolve("xed.cfg").toFile()
        );
    }

    public void actionPerformed(ActionEvent e) {
        Map<String, Object> lparam = new HashMap<>();
        lparam.put("view", editor);
        lparam.put("display", Boolean.TRUE);
        lparam.put("register", Boolean.TRUE);
        ServiceLocator.getInstance()
            .getEventsDispatcher().fireEvent(IView.DISPLAY_IN_DESKTOP_EVENT, lparam, null);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public CButton[] getButtons() {

        Image normal = new ResourceLoader(WorkspaceXMLEditor.class)
            .getResourceAsImage("images/editor.png");
        Image hover = new ResourceLoader(WorkspaceXMLEditor.class)
            .getResourceAsImage("images/editor.png");

        CButton bEditor = CButton.create(
            this,
            new ImageIcon(normal),
            new ImageIcon(hover),
            SHOW,
            "Clematis XML WYSIWYG Editor"
        );

        return new CButton[] {bEditor};
    }

    @Override
    public void load() throws IOException {
        try {
            this.config.load();
            this.editor.updateFont(
                new Font(
                    this.config.getString(CK_FONT_FACE),
                    this.config.getInt(CK_FONT_STYLE),
                    this.config.getInt(CK_FONT_SIZE)
                )
            );
        } catch (IOException e) {
            // ignore to defaults
        }
        this.editor.setWorkingDirectory(
            this.pluginContext.getUserDir().toFile()
        );
    }

    @Override
    public void save() throws IOException {
        Font font = this.editor.getTextPane().getFont();
        this.config.put(CK_FONT_FACE, font.getFamily());
        this.config.putInt(CK_FONT_SIZE, font.getSize());
        this.config.putInt(CK_FONT_STYLE, font.getStyle());
        try {
            this.config.store();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void reset() {

    }

    public static ResourceManager getResourceManager() {
        return ResourceManagerHolder.RESOURCE_MANAGER;
    }

    private static final class ResourceManagerHolder {
        private static final ResourceManager RESOURCE_MANAGER = new ResourceManager(WorkspaceXMLEditor.class);
    }
}

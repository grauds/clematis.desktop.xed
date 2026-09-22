package org.clematis.desktop.xed.actions;

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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import javax.swing.Action;

import org.clematis.desktop.xed.ResourceAnchor;

import lombok.extern.java.Log;


@Log
public class EditorActionsCollection {
    /**
     * Action property label
     */
    public static final String ACTION_TYPE = "ACTION_TYPE";

    protected final Map<String, Action> actions = new HashMap<>();

    protected void register(Action action) {
        String key = (String) action.getValue(Action.ACTION_COMMAND_KEY);
        actions.put(key, action);
    }

    protected String getResourceString(String nm) {
        String str;
        try {
            str = ResourceAnchor.getString(nm);
        } catch (MissingResourceException mre) {
            str = null;
        }
        return str;
    }

    protected URL getResource(String key) {
        String name = getResourceString(key);
        if (name != null) {
            return ResourceAnchor.class.getResource(name);
        }
        return null;
    }
}

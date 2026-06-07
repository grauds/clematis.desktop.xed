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
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a node in an XML document structure. Each node has a tag name,
 * optional text content, a parent node, and a list of child nodes.
 * <p>
 * This class provides functionality to manage XML nodes, such as adding,
 * removing, or retrieving children, and converting the node/subtree into
 * XML markup representation.
 */
@Getter
public class XmlNode {
    private final String tagName;
    @Setter
    private String textContent;
    private XmlNode parent;
    private final List<XmlNode> children = new ArrayList<>();

    public XmlNode(String tagName, String textContent) {
        this.tagName = tagName;
        this.textContent = textContent;
    }

    public boolean isTextNode() {
        return "text".equals(tagName);
    }

    public void addChild(XmlNode child) {
        child.parent = this;
        this.children.add(child);
    }

    public void addChildAt(int index, XmlNode child) {
        child.parent = this;
        this.children.add(index, child);
    }

    public void removeChild(XmlNode child) {
        this.children.remove(child);
        child.parent = null;
    }

    @Override
    public String toString() {
        return isTextNode() ? '"' + textContent + '"' : '<' + tagName + '>';
    }

    public String toXmlMarkup() {
        if (isTextNode()) {
            return textContent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(tagName).append('>');
        for (XmlNode child : children) {
            sb.append(child.toXmlMarkup());
        }
        sb.append("</").append(tagName).append('>');
        return sb.toString();
    }
}

package org.clematis.desktop.xed;

import java.util.ArrayList;
import java.util.List;

public class XmlNode {
    private String tagName;
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

    public String getTagName() { return tagName; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String text) { this.textContent = text; }
    public XmlNode getParent() { return parent; }
    public List<XmlNode> getChildren() { return children; }

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
        return isTextNode() ? "\"" + textContent + "\"" : "<" + tagName + ">";
    }

    public String toXmlMarkup() {
        if (isTextNode()) {
            return textContent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName).append(">");
        for (XmlNode child : children) {
            sb.append(child.toXmlMarkup());
        }
        sb.append("</").append(tagName).append(">");
        return sb.toString();
    }
}

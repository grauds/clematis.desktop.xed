package org.clematis.desktop.xed;

public class TextSegment {
    public final int startOffset;
    public final int length;
    public final XmlNode textNode;

    public TextSegment(int startOffset, int length, XmlNode textNode) {
        this.startOffset = startOffset;
        this.length = length;
        this.textNode = textNode;
    }
}

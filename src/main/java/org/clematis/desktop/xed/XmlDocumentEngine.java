package org.clematis.desktop.xed;

import java.util.ArrayList;
import java.util.List;

public class XmlDocumentEngine {
    private final XmlNode root;
    private final List<TextSegment> segments = new ArrayList<>();

    // The single source of truth for the entire editor instance
    private String flatText = "Type text here freely.";
    private final List<StyleInterval> activeStyles = new ArrayList<>();

    public XmlDocumentEngine(XmlNode root) {
        this.root = root;
        rebuildXmlTreeFromSpans();
    }

    public XmlNode getRoot() { return root; }
    public List<TextSegment> getSegments() { return segments; }

    private static class StyleInterval {
        int start, end;
        String type;

        StyleInterval(int start, int end, String type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }
    }

    // =========================================================================
    // SECTION 1: CORE DATA MUTATIONS
    // =========================================================================

    public synchronized void insertText(int globalOffset, String newText) {
        if (newText == null || newText.isEmpty()) return;

        flatText = flatText.substring(0, globalOffset) + newText + flatText.substring(globalOffset);
        int len = newText.length();

        for (StyleInterval interval : activeStyles) {
            if (globalOffset > interval.start && globalOffset < interval.end) {
                interval.end += len;
            } else if (globalOffset <= interval.start) {
                interval.start += len;
                interval.end += len;
            }
        }
        rebuildXmlTreeFromSpans();
    }

    public synchronized void deleteTextRange(int globalOffset, int length) {
        if (length <= 0 || globalOffset + length > flatText.length()) return;

        flatText = flatText.substring(0, globalOffset) + flatText.substring(globalOffset + length);

        List<StyleInterval> toRemove = new ArrayList<>();
        for (StyleInterval interval : activeStyles) {
            if (globalOffset <= interval.start && (globalOffset + length) >= interval.end) {
                toRemove.add(interval);
            } else {
                if (globalOffset > interval.start && globalOffset < interval.end) {
                    interval.end -= Math.min(length, interval.end - globalOffset);
                }
                if (globalOffset + length <= interval.start) {
                    interval.start -= length;
                    interval.end -= length;
                } else if (globalOffset < interval.start && globalOffset + length > interval.start) {
                    int overlap = (globalOffset + length) - interval.start;
                    interval.start = globalOffset;
                    interval.end -= overlap;
                }
            }
        }
        activeStyles.removeAll(toRemove);
        rebuildXmlTreeFromSpans();
    }

    public synchronized void wrapRangeInNode(int start, int end, String tagName) {
        if (start >= end || start < 0 || end > flatText.length()) return;

        // Verify if this specific style is uniformly present across the selection range
        boolean isUniformlyActive = true;
        for (int i = start; i < end; i++) {
            if (!isStyleActiveAtCharacter(i, tagName)) {
                isUniformlyActive = false;
                break;
            }
        }

        if (isUniformlyActive) {
            // TOGGLE OFF ACTION: Slice out or split the style intervals for this range
            List<StyleInterval> fragmentsToAdd = new ArrayList<>();
            List<StyleInterval> intervalsToRemove = new ArrayList<>();

            for (StyleInterval interval : activeStyles) {
                if (interval.type.equalsIgnoreCase(tagName)) {
                    if (interval.start < end && interval.end > start) {
                        intervalsToRemove.add(interval);
                        if (interval.start < start) {
                            fragmentsToAdd.add(new StyleInterval(interval.start, start, tagName));
                        }
                        if (interval.end > end) {
                            fragmentsToAdd.add(new StyleInterval(end, interval.end, tagName));
                        }
                    }
                }
            }
            activeStyles.removeAll(intervalsToRemove);
            activeStyles.addAll(fragmentsToAdd);
        } else {
            // TOGGLE ON ACTION: Absorb minor fragmented intersections into one unified style range
            activeStyles.removeIf(i -> i.type.equalsIgnoreCase(tagName) && i.start >= start && i.end <= end);
            activeStyles.add(new StyleInterval(start, end, tagName));
        }

        normalizeStyleSpans();
        rebuildXmlTreeFromSpans();
    }

    private boolean isStyleActiveAtCharacter(int offset, String tagName) {
        for (StyleInterval interval : activeStyles) {
            if (interval.type.equalsIgnoreCase(tagName) && offset >= interval.start && offset < interval.end) {
                return true;
            }
        }
        return false;
    }

    private void normalizeStyleSpans() {
        for (int i = 0; i < activeStyles.size(); i++) {
            StyleInterval a = activeStyles.get(i);
            for (int j = i + 1; j < activeStyles.size(); j++) {
                StyleInterval b = activeStyles.get(j);
                if (a.type.equalsIgnoreCase(b.type)) {
                    if (!(a.end < b.start || b.end < a.start)) {
                        a.start = Math.min(a.start, b.start);
                        a.end = Math.max(a.end, b.end);
                        activeStyles.remove(j);
                        j--;
                    }
                }
            }
        }
    }

    // =========================================================================
    // SECTION 2: TOP-DOWN CANONICAL PARSE BUILDER
    // =========================================================================

    synchronized void rebuildXmlTreeFromSpans() {
        root.getChildren().clear();
        segments.clear();

        if (flatText.isEmpty()) {
            return;
        }

        // Build the tree using top-down recursion from the document boundary limits
        int[] runningOffsetTracker = {0};
        buildTreeRecursive(root, 0, flatText.length(), activeStyles, runningOffsetTracker);
    }

    // Dominant sorting priority layer: Custom tags -> <b> -> <i> -> <u>
    private StyleInterval selectDominantStyle(List<StyleInterval> intervals) {
        StyleInterval choice = intervals.get(0);

        for (StyleInterval interval : intervals) {
            boolean isChoiceCustom = !isCoreStyleTag(choice.type);
            boolean isIntervalCustom = !isCoreStyleTag(interval.type);

            // Rule 1: Custom tags always take precedence over core style tags
            if (isIntervalCustom && !isChoiceCustom) {
                choice = interval;
            }
            // Rule 2: If both are custom tags, choose the one with the larger character span
            else if (isIntervalCustom && isChoiceCustom) {
                if ((interval.end - interval.start) > (choice.end - choice.start)) {
                    choice = interval;
                }
            }
            // Rule 3: If both are core styles, fall back to standard styling nesting layers (b > i > u)
            else if (!isIntervalCustom && !isChoiceCustom) {
                if ("b".equalsIgnoreCase(interval.type) && !"b".equalsIgnoreCase(choice.type)) {
                    choice = interval;
                } else if ("i".equalsIgnoreCase(interval.type) && !"b".equalsIgnoreCase(choice.type) && !"i".equalsIgnoreCase(choice.type)) {
                    choice = interval;
                } else if ("u".equalsIgnoreCase(interval.type) && !"b".equalsIgnoreCase(choice.type) && !"i".equalsIgnoreCase(choice.type) && !"u".equalsIgnoreCase(choice.type)) {
                    choice = interval;
                }
            }
        }
        return choice;
    }

    // Helper to distinguish core formatting flags from dynamic schema markers
    private boolean isCoreStyleTag(String tag) {
        return "b".equalsIgnoreCase(tag) || "i".equalsIgnoreCase(tag) || "u".equalsIgnoreCase(tag);
    }

    private void buildTreeRecursive(XmlNode parentNode, int rangeStart, int rangeEnd, List<StyleInterval> availableStyles, int[] runningOffsetTracker) {
        if (rangeStart >= rangeEnd) return;

        // 1. Isolate style intervals that overlap with the current character window range
        List<StyleInterval> localStyles = new ArrayList<>();
        for (StyleInterval style : availableStyles) {
            int intersectStart = Math.max(style.start, rangeStart);
            int intersectEnd = Math.min(style.end, rangeEnd);
            if (intersectStart < intersectEnd) {
                localStyles.add(new StyleInterval(intersectStart, intersectEnd, style.type));
            }
        }

        // 2. Base Case: If no styles apply to this range, create a plain text leaf node
        if (localStyles.isEmpty()) {
            String textSlice = flatText.substring(rangeStart, rangeEnd);
            XmlNode textLeaf = new XmlNode("text", textSlice);
            parentNode.addChild(textLeaf);
            segments.add(new TextSegment(runningOffsetTracker[0], textSlice.length(), textLeaf));
            runningOffsetTracker[0] += textSlice.length();
            return;
        }

        // 3. Select the dominant wrapper style based on type hierarchy rules (b > i > others)
        StyleInterval dominantStyle = selectDominantStyle(localStyles);

        // 4. Split the current range into three segments based on the dominant style's boundaries
        // Segment A: Text before the dominant style begins
        if (rangeStart < dominantStyle.start) {
            buildTreeRecursive(parentNode, rangeStart, dominantStyle.start, localStyles, runningOffsetTracker);
        }

        // Segment B: Inside the dominant style wrapper element
        XmlNode dominantStyleWrapperNode = new XmlNode(dominantStyle.type, null);
        parentNode.addChild(dominantStyleWrapperNode);

        // Remove the dominant style from child loops to avoid infinite recursion loops
        List<StyleInterval> subStyles = new ArrayList<>();
        for (StyleInterval s : localStyles) {
            if (!(s.type.equalsIgnoreCase(dominantStyle.type) && s.start == dominantStyle.start && s.end == dominantStyle.end)) {
                subStyles.add(s);
            }
        }
        buildTreeRecursive(dominantStyleWrapperNode, dominantStyle.start, dominantStyle.end, subStyles, runningOffsetTracker);

        // Segment C: Text after the dominant style ends
        if (dominantStyle.end < rangeEnd) {
            buildTreeRecursive(parentNode, dominantStyle.end, rangeEnd, localStyles, runningOffsetTracker);
        }
    }

   public synchronized void deleteStructuralNode(XmlNode nodeToDelete) {
        if (nodeToDelete == root || nodeToDelete.isTextNode()) return;
        activeStyles.removeIf(interval -> interval.type.equalsIgnoreCase(nodeToDelete.getTagName()));
        rebuildXmlTreeFromSpans();}public void rebuildLinearSegments() {
        // Handled automatically inside the top-down constructor layout pass
        }
}


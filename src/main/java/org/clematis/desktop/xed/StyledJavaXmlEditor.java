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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.hyperrealm.kiwi.ui.KPanel;

import lombok.Getter;

/**
 * A specialized editor component that accommodates the editing and visualization
 * of Java and XML content with styled features. It serves as a Swing-based editor interface
 * integrating syntax highlighting, tree visualization, and XML structure preview.
 * <p>
 * This class extends {@code javax.swing.JFrame} and includes various Swing components
 * and custom logic for enhancing the user experience when working with Java and XML documents.
 */
public class StyledJavaXmlEditor extends KPanel {

    private XmlDocumentEngine engine;
    private JTextPane textPane;
    private JTree visualTree;
    private JTextArea xmlPreviewArea;
    private EditorDocumentFilter docFilter;

    private final Map<XmlNode, DefaultMutableTreeNode> nodeToUiMap = new HashMap<>();
    private int nextTargetCaretPos = 0;

    @Getter
    private File workingDirectory;

    @SuppressWarnings("checkstyle:MagicNumber")
    public StyledJavaXmlEditor() {
        JSplitPane verticalSplit = getJSplitPane();

        setLayout(new BorderLayout());
        add(verticalSplit, BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.NORTH);

        setupTreeContextMenu();

        getTextPane().addCaretListener(e -> {
            if (getXmlPreviewArea().hasFocus() || getVisualTree().hasFocus()) {
                return;
            }
            highlightNodeFromOffset(e.getDot());
        });

        setSize(1000, 700);
        refreshUiFromModel();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private JSplitPane getJSplitPane() {
        JSplitPane horizontalSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(getVisualTree()), new JScrollPane(getTextPane())
        );
        horizontalSplit.setDividerLocation(550);
        horizontalSplit.setContinuousLayout(true);

        JSplitPane verticalSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, horizontalSplit, new JScrollPane(getXmlPreviewArea())
        );
        verticalSplit.setDividerLocation(450);
        verticalSplit.setContinuousLayout(true);
        return verticalSplit;
    }

    // Bypasses Swing's Highlighter completely to paint precise bounding boxes
    @SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals", "checkstyle:NestedIfDepth"})
    private void drawCustomNodeBackgrounds(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Define distinct background and border styles for custom nodes
        Color bgOrange = new Color(255, 235, 204);
        Color borderOrange = new Color(255, 153, 51);
        Color bgBlue = new Color(230, 245, 255);
        Color borderBlue = new Color(51, 153, 255);

        synchronized (getEngine()) {
            // Read active text segments directly from the engine cache
            for (TextSegment seg : getEngine().getSegments()) {
                XmlNode current = seg.textNode().getParent();
                String customTag = null;

                // Identify if the segment is wrapped in a custom node
                while (current != null) {
                    String tag = current.getTagName();
                    if (!"b".equalsIgnoreCase(tag)
                        && !"i".equalsIgnoreCase(tag)
                        && !"u".equalsIgnoreCase(tag)
                        && current != getEngine().getRoot()
                    ) {
                        customTag = tag.toLowerCase();
                        break;
                    }
                    current = current.getParent();
                }

                // If a custom node is found, calculate its exact coordinates on screen
                if (customTag != null) {
                    try {
                        int p0 = seg.startOffset();
                        int p1 = seg.startOffset() + seg.length();

                        Rectangle2D r0 = getTextPane().modelToView2D(p0);
                        Rectangle2D r1 = getTextPane().modelToView2D(p1);

                        if (r0 != null && r1 != null) {
                            Color activeBg = "header".equals(customTag) ? bgBlue : bgOrange;
                            Color activeBorder = "header".equals(customTag) ? borderBlue : borderOrange;

                            int x = (int) r0.getX();
                            int y = (int) r0.getY();
                            int height = (int) r0.getHeight();
                            int width = (int) (r1.getX() - r0.getX());

                            if (width > 0 && height > 0) {
                                // Draw the precise rounded background
                                g2d.setColor(activeBg);
                                g2d.fillRoundRect(x, y + 1, width, height - 2, 4, 4);

                                // Draw the crisp outer outline border
                                g2d.setColor(activeBorder);
                                g2d.setStroke(new BasicStroke(1.0f));
                                g2d.drawRoundRect(x, y + 1, width - 1, height - 3, 4, 4);
                            }
                        }
                    } catch (BadLocationException e) {
                        // Suppress out-of-bounds painting artifacts during transitions
                    }
                }
            }
        }
    }

    private JToolBar createToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton boldBtn = new JButton("Bold (<b>)");
        boldBtn.addActionListener(_ -> applyTagSelection("b"));

        JButton italicBtn = new JButton("Italic (<i>)");
        italicBtn.addActionListener(_ -> applyTagSelection("i"));

        JButton underlineBtn = new JButton("Underline (<u>)");
        underlineBtn.addActionListener(_ -> applyTagSelection("u"));

        JButton customBtn = new JButton("Insert Custom Node...");
        customBtn.addActionListener(_ -> {
            String customTag = JOptionPane.showInputDialog(
                this,
                "Enter custom XML tag name:",
                "Custom Node",
                JOptionPane.PLAIN_MESSAGE
            );
            if (customTag != null && !customTag.trim().isEmpty()) {
                applyTagSelection(customTag.trim().toLowerCase());
            }
        });

        toolBar.add(boldBtn);
        toolBar.add(italicBtn);
        toolBar.add(underlineBtn);
        toolBar.addSeparator();
        toolBar.add(customBtn);
        return toolBar;
    }

    private void applyTagSelection(String tagName) {
        int start = getTextPane().getSelectionStart();
        int end = getTextPane().getSelectionEnd();
        if (start == end) {
            JOptionPane.showMessageDialog(this, "Please highlight a text range first.");
            return;
        }
        nextTargetCaretPos = start;
        getEngine().wrapRangeInNode(start, end, tagName);
        refreshUiFromModel();
    }

    private void setupTreeContextMenu() {
        JPopupMenu popupMenu = getJPopupMenu();

        getVisualTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }
            private void handlePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = getVisualTree().getClosestRowForLocation(e.getX(), e.getY());
                    getVisualTree().setSelectionRow(row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private JPopupMenu getJPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Structural Node");

        deleteItem.addActionListener(_ -> {
            TreePath selectionPath = getVisualTree().getSelectionPath();
            if (selectionPath != null) {
                DefaultMutableTreeNode uiNode
                    = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                XmlNode xmlNode = (XmlNode) uiNode.getUserObject();
                if (xmlNode == getEngine().getRoot() || xmlNode.isTextNode()) {
                    return;
                }
                nextTargetCaretPos = getTextPane().getCaretPosition();
                getEngine().deleteStructuralNode(xmlNode);
                refreshUiFromModel();
            }
        });
        popupMenu.add(deleteItem);
        return popupMenu;
    }

    private void refreshUiFromModel() {
        getDocFilter().setEnabled(false);
        StyledDocument doc = getTextPane().getStyledDocument();

        try {
            doc.remove(0, doc.getLength());
            for (TextSegment seg : getEngine().getSegments()) {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                XmlNode current = seg.textNode().getParent();

                while (current != null) {
                    String tag = current.getTagName();
                    if ("b".equalsIgnoreCase(tag)) {
                        StyleConstants.setBold(attrs, true);
                    } else if ("i".equalsIgnoreCase(tag)) {
                        StyleConstants.setItalic(attrs, true);
                    } else if ("u".equalsIgnoreCase(tag)) {
                        StyleConstants.setUnderline(attrs, true);
                    }
                    current = current.getParent();
                }
                doc.insertString(doc.getLength(), seg.textNode().getTextContent(), attrs);
            }
        } catch (BadLocationException ex) {
            // skip bad location exceptions
        }

        int safeCaretPos = Math.min(nextTargetCaretPos, doc.getLength());
        getTextPane().setCaretPosition(safeCaretPos);

        nodeToUiMap.clear();
        DefaultMutableTreeNode visualRoot = buildTreeUiModel(getEngine().getRoot());
        getVisualTree().setModel(new DefaultTreeModel(visualRoot));

        for (int i = 0; i < getVisualTree().getRowCount(); i++) {
            getVisualTree().expandRow(i);
        }

        getXmlPreviewArea().setText(getEngine().getRoot().toXmlMarkup());
        getDocFilter().setEnabled(true);
        getTextPane().repaint(); // Force clean redraw frame updates
    }

    private DefaultMutableTreeNode buildTreeUiModel(XmlNode xmlNode) {
        DefaultMutableTreeNode uiNode = new DefaultMutableTreeNode(xmlNode);
        nodeToUiMap.put(xmlNode, uiNode);

        for (XmlNode child : xmlNode.getChildren()) {
            uiNode.add(buildTreeUiModel(child));
        }
        return uiNode;
    }

    private void highlightNodeFromOffset(int offset) {
        for (TextSegment seg : getEngine().getSegments()) {
            if (offset >= seg.startOffset()
                && offset <= seg.startOffset() + seg.length()
            ) {
                XmlNode xmlTarget = seg.textNode().getParent() != null
                    ? seg.textNode().getParent() : seg.textNode();
                DefaultMutableTreeNode uiTarget = nodeToUiMap.get(xmlTarget);

                if (uiTarget != null) {
                    TreePath path = new TreePath(uiTarget.getPath());
                    getVisualTree().setSelectionPath(path);
                }
                break;
            }
        }
    }

    public JTextPane getTextPane() {
        if (textPane == null) {
            // Override paintComponent to render custom nodes directly from data metrics
            textPane = new JTextPane() {
                @Override
                protected void paintComponent(Graphics g) {
                    // Draw custom structural node boxes first (Background layer)
                    drawCustomNodeBackgrounds(g);
                    // Draw standard text characters and core font properties above backgrounds
                    super.paintComponent(g);
                }
            };
        }
        return textPane;
    }

    public void updateFont(Font newFont) {
        getTextPane().setFont(newFont);
        revalidate(); repaint();
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public JTree getVisualTree() {
        if (visualTree == null) {
            visualTree = new JTree(new DefaultMutableTreeNode("root"));
        }
        return visualTree;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public JTextArea getXmlPreviewArea() {
        if (xmlPreviewArea == null) {
            xmlPreviewArea = new JTextArea(6, 20);
            xmlPreviewArea.setEditable(false);
            xmlPreviewArea.setLineWrap(true);
            xmlPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            xmlPreviewArea.setBackground(new Color(245, 245, 245));
        }
        return xmlPreviewArea;
    }

    public EditorDocumentFilter getDocFilter() {
        if (docFilter == null) {
            docFilter = new EditorDocumentFilter(
                getEngine(),
                this::refreshUiFromModel,
                pos -> nextTargetCaretPos = pos
            );
            ((AbstractDocument) getTextPane().getStyledDocument()).setDocumentFilter(docFilter);

        }
        return docFilter;
    }

    public XmlDocumentEngine getEngine() {
        if (engine == null) {
            XmlNode rootNode = new XmlNode("root", null);
            rootNode.addChild(new XmlNode(
                "text",
                "Type text here freely.Type text here <header>freely."
                    + "Type text here her</header>e freely."
            ));

            engine = new XmlDocumentEngine(rootNode);
            engine.rebuildXmlTreeFromSpans();
        }
        return engine;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}

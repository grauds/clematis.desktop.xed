package org.clematis.desktop.xed;

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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class StyledJavaXmlEditor extends JFrame {
    private final XmlDocumentEngine engine;
    private final JTextPane textPane;
    private final JTree visualTree;
    private final JTextArea xmlPreviewArea;
    private final EditorDocumentFilter docFilter;
    private final Map<XmlNode, DefaultMutableTreeNode> nodeToUiMap = new HashMap<>();
    private int nextTargetCaretPos = 0;

    public StyledJavaXmlEditor() {
        setTitle("Normalized Structural XML WYSIWYG Editor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        XmlNode rootNode = new XmlNode("root", null);
        rootNode.addChild(new XmlNode("text", "Type text here freely.Type text here <header>freely.Type text here her</header>e freely."));
        engine = new XmlDocumentEngine(rootNode);
        engine.rebuildXmlTreeFromSpans();

        // CRITICAL UPDATE: Override paintComponent to render custom nodes directly from data metrics
        textPane = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                // Tier 1: Draw custom structural node boxes first (Background layer)
                drawCustomNodeBackgrounds(g);
                // Tier 2: Draw standard text characters and core font properties above backgrounds
                super.paintComponent(g);
            }
        };

        visualTree = new JTree(new DefaultMutableTreeNode("root"));

        xmlPreviewArea = new JTextArea(6, 20);
        xmlPreviewArea.setEditable(false);
        xmlPreviewArea.setLineWrap(true);
        xmlPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        xmlPreviewArea.setBackground(new Color(245, 245, 245));

        docFilter = new EditorDocumentFilter(
            engine,
            this::refreshUiFromModel,
            pos -> nextTargetCaretPos = pos
        );
        ((AbstractDocument) textPane.getStyledDocument()).setDocumentFilter(docFilter);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textPane), new JScrollPane(visualTree));
        horizontalSplit.setDividerLocation(550);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontalSplit, new JScrollPane(xmlPreviewArea));
        verticalSplit.setDividerLocation(450);

        add(verticalSplit, BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.NORTH);

        setupTreeContextMenu();

        textPane.addCaretListener(e -> {
            if (xmlPreviewArea.hasFocus() || visualTree.hasFocus()) return;
            highlightNodeFromOffset(e.getDot());
        });

        refreshUiFromModel();
    }

    // HIGH-UTILITY TECHNIQUE: Bypasses Swing's Highlighter completely to paint precise bounding boxes
    private void drawCustomNodeBackgrounds(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Define distinct background and border styles for custom nodes
        Color bgOrange = new Color(255, 235, 204);
        Color borderOrange = new Color(255, 153, 51);
        Color bgBlue = new Color(230, 245, 255);
        Color borderBlue = new Color(51, 153, 255);

        synchronized (engine) {
            // Read active text segments directly from the engine cache
            for (TextSegment seg : engine.getSegments()) {
                XmlNode current = seg.textNode.getParent();
                String customTag = null;

                // Identify if the segment is wrapped in a custom node
                while (current != null) {
                    String tag = current.getTagName();
                    if (!"b".equalsIgnoreCase(tag) && !"i".equalsIgnoreCase(tag) && !"u".equalsIgnoreCase(tag) && current != engine.getRoot()) {
                        customTag = tag.toLowerCase();
                        break;
                    }
                    current = current.getParent();
                }

                // If a custom node is found, calculate its exact coordinates on screen
                if (customTag != null) {
                    try {
                        int p0 = seg.startOffset;
                        int p1 = seg.startOffset + seg.length;

                        Rectangle2D r0 = textPane.modelToView2D(p0);
                        Rectangle2D r1 = textPane.modelToView2D(p1);

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
        boldBtn.addActionListener(e -> applyTagSelection("b"));

        JButton italicBtn = new JButton("Italic (<i>)");
        italicBtn.addActionListener(e -> applyTagSelection("i"));

        JButton underlineBtn = new JButton("Underline (<u>)");
        underlineBtn.addActionListener(e -> applyTagSelection("u"));

        JButton customBtn = new JButton("Insert Custom Node...");
        customBtn.addActionListener(e -> {
            String customTag = JOptionPane.showInputDialog(this, "Enter custom XML tag name:", "Custom Node", JOptionPane.PLAIN_MESSAGE);
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
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        if (start == end) {
            JOptionPane.showMessageDialog(this, "Please highlight a text range first.");
            return;
        }
        nextTargetCaretPos = start;
        engine.wrapRangeInNode(start, end, tagName);
        refreshUiFromModel();
    }

    private void setupTreeContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Structural Node");

        deleteItem.addActionListener(e -> {
            TreePath selectionPath = visualTree.getSelectionPath();
            if (selectionPath != null) {
                DefaultMutableTreeNode uiNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                XmlNode xmlNode = (XmlNode) uiNode.getUserObject();
                if (xmlNode == engine.getRoot() || xmlNode.isTextNode()) return;

                nextTargetCaretPos = textPane.getCaretPosition();
                engine.deleteStructuralNode(xmlNode);
                refreshUiFromModel();
            }
        });
        popupMenu.add(deleteItem);

        visualTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { handlePopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { handlePopup(e); }
            private void handlePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = visualTree.getClosestRowForLocation(e.getX(), e.getY());
                    visualTree.setSelectionRow(row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void refreshUiFromModel() {
        docFilter.setEnabled(false);
        StyledDocument doc = textPane.getStyledDocument();

        try {
            doc.remove(0, doc.getLength());
            for (TextSegment seg : engine.getSegments()) {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                XmlNode current = seg.textNode.getParent();

                while (current != null) {
                    String tag = current.getTagName();
                    if ("b".equalsIgnoreCase(tag)) StyleConstants.setBold(attrs, true);
                    else if ("i".equalsIgnoreCase(tag)) StyleConstants.setItalic(attrs, true);
                    else if ("u".equalsIgnoreCase(tag)) StyleConstants.setUnderline(attrs, true);
                    current = current.getParent();
                }
                doc.insertString(doc.getLength(), seg.textNode.getTextContent(), attrs);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        int safeCaretPos = Math.min(nextTargetCaretPos, doc.getLength());
        textPane.setCaretPosition(safeCaretPos);

        nodeToUiMap.clear();
        DefaultMutableTreeNode visualRoot = buildTreeUiModel(engine.getRoot());
        visualTree.setModel(new DefaultTreeModel(visualRoot));

        for (int i = 0; i < visualTree.getRowCount(); i++) {
            visualTree.expandRow(i);
        }

        xmlPreviewArea.setText(engine.getRoot().toXmlMarkup());
        docFilter.setEnabled(true);
        textPane.repaint(); // Force clean redraw frame updates
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
        for (TextSegment seg : engine.getSegments()) {
            if (offset >= seg.startOffset && offset <= seg.startOffset + seg.length) {
                XmlNode xmlTarget = seg.textNode.getParent() != null ? seg.textNode.getParent() : seg.textNode;
                DefaultMutableTreeNode uiTarget = nodeToUiMap.get(xmlTarget);

                if (uiTarget != null) {
                    TreePath path = new TreePath(uiTarget.getPath());
                    visualTree.setSelectionPath(path);
                }
                break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StyledJavaXmlEditor().setVisible(true));
    }
}

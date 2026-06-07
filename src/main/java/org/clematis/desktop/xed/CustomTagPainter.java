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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 * A custom highlight painter implementation for rendering tags in a text component
 * with a specified background color and border. This painter supports visualizing
 * single-line and multi-line tag highlights in a styled manner.
 * <p>
 * The CustomTagPainter class implements the Highlighter.HighlightPainter interface,
 * and is designed to be used in scenarios where custom tag highlights need to be
 * displayed in swing-based JTextComponent elements.
 * <p>
 * The paint method is overridden to ensure that the global document offsets
 * (offs0 and offs1) match the exact start and end of the custom tag being highlighted.
 * The method calculates the precise rectangular bounds for the highlight and renders
 * it with rounded corners using the specified background and border colors.
 * <p>
 * Features:
 * - Handles single-line highlights by rendering a rounded rectangle spanning the line.
 * - Handles multi-line highlights by rendering rounded rectangles for each line
 *   spanned by the tag.
 * - Dynamically calculates and renders intermediate line highlights for tags wrapping
 *   across multiple lines.
 * <p>
 * Constructor Parameters:
 * - backgroundColor: The color used for filling the highlight background.
 * - borderColor: The color used for the border around the highlight.
 * <p>
 * This class relies on precise layout and rendering behavior of the JTextComponent
 * and is dependent on its model-to-view coordinate mapping.
 */
public class CustomTagPainter implements Highlighter.HighlightPainter {
    private final Color backgroundColor;
    private final Color borderColor;

    public CustomTagPainter(Color backgroundColor, Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    /**
     * Overriding the base paint() method ensures we ALWAYS receive the true global
     * document offsets (offs0 and offs1) that match the exact start and end of your custom tag.
     */
    @SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:ReturnCount"})
    @Override
    public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        if (offs0 >= offs1) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            // Get the precise on-screen coordinates of our text boundary markers
            Rectangle2D rStart = c.modelToView2D(offs0);
            Rectangle2D rEnd = c.modelToView2D(offs1);

            if (rStart == null || rEnd == null) {
                return;
            }

            Insets insets = c.getInsets();
            int leftMarginX = (insets != null) ? insets.left : 0;
            int rightMarginWidth = c.getWidth() - ((insets != null) ? insets.right : 0);
            int fontHeight = (int) rStart.getHeight();

            // =================================================================
            // CASE A: SINGLE LINE (The tag starts and ends on the same line row)
            // =================================================================
            if (Math.abs(rStart.getY() - rEnd.getY()) < 4.0) {
                int x = (int) rStart.getX();
                int y = (int) rStart.getY();
                int width = (int) (rEnd.getX() - rStart.getX());

                if (width <= 0) {
                    return;
                }

                // Draw the precise background rectangle clip
                g2d.setColor(backgroundColor);
                g2d.fillRoundRect(x, y + 1, width, fontHeight - 2, 4, 4);

                // Draw the clean border edge
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(x, y + 1, width - 1, fontHeight - 3, 4, 4);
            } else {
                // =================================================================
                // CASE B: MULTI-LINE (The tag wraps across lines and ends mid-line)
                // =================================================================
                int yStart = (int) rStart.getY();
                int yEnd = (int) rEnd.getY();

                // Row 1: Draw from the start index to the right edge of the text pane line
                int wStart = rightMarginWidth - (int) rStart.getX();
                if (wStart > 0) {
                    g2d.setColor(backgroundColor);
                    g2d.fillRoundRect((int) rStart.getX(), yStart + 1, wStart, fontHeight - 2, 4, 4);
                    g2d.setColor(borderColor);
                    g2d.drawRoundRect((int) rStart.getX(), yStart + 1, wStart - 1, fontHeight - 3, 4, 4);
                }

                // Intermediate Rows: Draw full full-width blocks for any lines trapped inside
                int nextLineY = yStart + fontHeight;
                while (nextLineY < yEnd - 2) {
                    int wMid = rightMarginWidth - leftMarginX;
                    g2d.setColor(backgroundColor);
                    g2d.fillRoundRect(leftMarginX, nextLineY + 1, wMid, fontHeight - 2, 4, 4);
                    g2d.setColor(borderColor);
                    g2d.drawRoundRect(leftMarginX, nextLineY + 1, wMid - 1, fontHeight - 3, 4, 4);

                    nextLineY += fontHeight;
                }

                // Final Row: Draw from left margin and stop EXACTLY at rEnd.getX() mid-line
                int wEnd = (int) rEnd.getX() - leftMarginX;
                if (wEnd > 0) {
                    g2d.setColor(backgroundColor);
                    g2d.fillRoundRect(leftMarginX, yEnd + 1, wEnd, fontHeight - 2, 4, 4);
                    g2d.setColor(borderColor);
                    g2d.drawRoundRect(leftMarginX, yEnd + 1, wEnd - 1, fontHeight - 3, 4, 4);
                }
            }
        } catch (BadLocationException e) {
            // Layout indices protection catch
        }
    }
}


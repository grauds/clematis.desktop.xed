package org.clematis.desktop.xed;

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

                if (width <= 0) return;

                // Draw the precise background rectangle clip
                g2d.setColor(backgroundColor);
                g2d.fillRoundRect(x, y + 1, width, fontHeight - 2, 4, 4);

                // Draw the clean border edge
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(x, y + 1, width - 1, fontHeight - 3, 4, 4);
            }
            // =================================================================
            // CASE B: MULTI-LINE (The tag wraps across lines and ends mid-line)
            // =================================================================
            else {
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


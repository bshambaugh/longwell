package edu.mit.simile.longwell.renderer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import edu.mit.simile.longwell.RenderingException;

/**
 * Render a map marker (a colored rounded rectangle with a pointer on
 * the bottom) into an image.
 */
public class MapMarkerRenderer implements Renderer {

    final static private double s_xCornerRadius = 10;
    final static private double s_yCornerRadius = 10;
    final static private double s_pointerHeight = 9;
    final static private double s_maxBubbleWidth = 37;
    final static private double s_bubbleHeight = 23;

    final static private Font s_font = new Font("Georgia", Font.BOLD, 12);

    final static private Color[] s_colorCharts = new Color[] { 
        new Color(254, 119, 107), 
        new Color(33, 126, 174),
        new Color(59, 137, 109), 
        new Color(252, 103, 194) 
    };

    public void render(Graphics2D g2d, Rectangle2D crop, HttpServletRequest request) throws RenderingException {
        String label = request.getParameter("label");
        if (label == null) {
            label = "";
        }

        /*
         * Determine the colors
         */

        Color[] colors = null;
        int colorCount = 0;

        String rgbString = request.getParameter("rgb");
        if (rgbString != null) {
            String[] ss = StringUtils.splitPreserveAllTokens(rgbString, ',');
            
            colors = new Color[] { 
                new Color(
                    Integer.parseInt(ss[0]), 
                    Integer.parseInt(ss[1]), 
                    Integer.parseInt(ss[2]))
            };
            colorCount = 1;
        } else {
            colors = new Color[s_colorCharts.length];
            
            String colorCodeString = request.getParameter("colorCode");
            if (colorCodeString != null) {
                String[] ss = StringUtils.splitPreserveAllTokens(colorCodeString, ',');
                for (int i = 0; i < ss.length; i++) {
                    try {
                        int index = Math.max(0, Math.min(s_colorCharts.length - 1, Integer.parseInt(ss[i])));
    
                        colors[index] = s_colorCharts[index];
                    } catch (Exception e) {
                        //
                    }
                }
            }
    
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] != null) {
                    colorCount++;
                }
            }
            if (colorCount == 0) {
                colors = new Color[] { s_colorCharts[0] };
                colorCount++;
            } else {
                Color[] colors2 = new Color[colorCount];
                int i = 0;
                for (int j = 0; j < colors.length; j++) {
                    if (colors[j] != null) {
                        colors2[i++] = colors[j];
                    }
                }
                colors = colors2;
            }
        }
        
        /*
         * Get dimensions, create paths
         */
        g2d.setFont(s_font);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font font = g2d.getFont();
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D labelBounds = font.getStringBounds(label, frc);
        LineMetrics lm = font.getLineMetrics(label, frc);

        double bubbleWidth = Math.min(s_maxBubbleWidth, labelBounds.getWidth() + 2 * s_xCornerRadius);

        RoundRectangle2D rr = new RoundRectangle2D.Double((crop.getWidth() - bubbleWidth) / 2, crop.getHeight()
                - s_bubbleHeight - s_pointerHeight - 1, bubbleWidth, s_bubbleHeight, s_xCornerRadius, s_yCornerRadius);

        GeneralPath path = new GeneralPath();
        path.moveTo((float) (rr.getMinX() + s_xCornerRadius / 2), (float) (rr.getMinY() + s_yCornerRadius));
        path.lineTo((float) (rr.getMaxX() - s_xCornerRadius / 2), (float) (rr.getMinY() + s_yCornerRadius));
        path.lineTo((float) rr.getCenterX(), (float) (rr.getMaxY() + s_pointerHeight - 1));
        path.closePath();

        Area area = new Area(path);
        area.add(new Area(rr));

        path = new GeneralPath();
        path.append(area.getPathIterator(new AffineTransform()), false);

        /*
         * Fill outline
         */
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

        if (colorCount < 4) {
            g2d.setColor(colors[0]);
            g2d.fill(path);
            if (colorCount == 2) {
                GeneralPath path2 = new GeneralPath();
                path2.moveTo((float) rr.getMinX(), (float) rr.getMaxY());
                path2.lineTo((float) rr.getMinX(), (float) rr.getMinY());
                path2.lineTo((float) rr.getMaxX(), (float) rr.getMinY());
                path2.closePath();

                g2d.setClip(path);
                g2d.setColor(colors[1]);
                g2d.fill(path2);

                g2d.setClip(crop);
            } else if (colorCount == 3) {
                g2d.setClip(path);

                g2d.setColor(colors[1]);
                g2d.fill(new Rectangle2D.Double(rr.getMinX(), rr.getMinY(), rr.getWidth() / 3, rr.getHeight()));

                g2d.setColor(colors[2]);
                g2d.fill(new Rectangle2D.Double(rr.getMaxX() - rr.getWidth() / 3, rr.getMinY(), rr.getWidth() / 3, rr
                        .getHeight()));

                g2d.setClip(crop);
            }
        } else {
            g2d.setColor(Color.WHITE);
            g2d.fill(path);
        }
        g2d.setComposite(AlphaComposite.SrcOver);

        /*
         * Draw outline
         */
        Color lineColor = Color.BLACK;
        g2d.setColor(lineColor);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(path);

        /*
         * Draw label
         */
        g2d.setClip(crop);

        float x = (float) ((crop.getWidth() - labelBounds.getWidth()) / 2);
        float y = (float) (crop.getHeight() - s_pointerHeight - s_yCornerRadius / 2 - lm.getDescent());

        g2d.drawString(label, x, y);
    }
}

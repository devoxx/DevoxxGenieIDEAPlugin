package com.devoxx.genie.ui.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public final class RoundBorder extends LineBorder {

    private final int radius;

    public RoundBorder(@NotNull Color color,
                       int thickness,
                       int radius) {
        super(color, thickness, true);
        this.radius = radius;
    }

    @Override
    public void paintBorder(@Nullable Component component,
                            @Nullable Graphics graphics,
                            int x, int y, int width, int height) {

        if (this.thickness > 0 && graphics instanceof Graphics2D g2d) {
            Color oldColor = graphics.getColor();
            g2d.setColor(this.lineColor);
            Shape outer;
            Shape inner;
            int offs = this.thickness;
            int size = offs + offs;
            float arc = (float) this.radius * (float) 2;
            outer = new RoundRectangle2D.Float(x, y, width, height, arc, arc);
            inner = new RoundRectangle2D.Float((x + offs), (y + offs), (width - size), (height - size), arc, arc);

            Path2D path = new Path2D.Float(0);
            path.append(outer, false);
            path.append(inner, false);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fill(path);
            graphics.setColor(oldColor);
        }

    }
}

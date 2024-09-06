package com.devoxx.genie.ui.component;

import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

public class GlowingBorder extends AbstractBorder {
    private final Color glowColor;
    private final int glowWidth = 4;

    @Getter
    @Setter
    private float alpha = 0.5f;

    public GlowingBorder(Color glowColor) {
        this.glowColor = glowColor;
    }

    @Override
    public void paintBorder(Component c, @NotNull Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arcWidth = 15;
        int arcHeight = 15;
        RoundRectangle2D.Float outer = new RoundRectangle2D.Float(x, y, width - 1, height - 1, arcWidth, arcHeight);
        RoundRectangle2D.Float inner = new RoundRectangle2D.Float(x + glowWidth, y + glowWidth, width - glowWidth * 2 - 1, height - glowWidth * 2 - 1, arcWidth, arcHeight);

        Area area = new Area(outer);
        area.subtract(new Area(inner));

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(glowColor);
        g2.fill(area);

        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return JBUI.insets(glowWidth);
    }

}

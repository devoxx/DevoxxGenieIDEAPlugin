package com.devoxx.genie.ui.component;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ContextPopupMenu {
    public void show(@NotNull JComponent component,
                     @NotNull JBPopup popup,
                     Integer width,
                     Integer offset) {
        if (!component.isShowing()) {
            return;
        }

        final RelativePoint northWest = new RelativePoint(component, new Point());
        popup.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(@NotNull LightweightWindowEvent event) {
                JBPopup popup = event.asPopup();
                Point point = new Point(popup.getLocationOnScreen());
                point.y = offset - popup.getSize().height;
                Dimension dimension = new Dimension(width, popup.getSize().height);
                popup.setSize(dimension);
                popup.setMinimumSize(dimension);
                popup.setLocation(point);
            }
        });

        // Ensure this is on the EDT
        SwingUtilities.invokeLater(() -> popup.show(northWest));
    }
}

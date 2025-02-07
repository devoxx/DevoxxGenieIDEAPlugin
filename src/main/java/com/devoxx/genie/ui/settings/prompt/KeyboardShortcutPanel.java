package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

public class KeyboardShortcutPanel extends JPanel {
    private final JCheckBox ctrlCheckBox;
    private final JCheckBox altCheckBox;
    private final JCheckBox shiftCheckBox;
    private final JCheckBox metaCheckBox;
    private final JComboBox<String> keyCombo;
    private final ShortcutChangeListener listener;
    private final Project project;

    public interface ShortcutChangeListener {
        void onShortcutChanged(String shortcut);
    }

    public KeyboardShortcutPanel(Project project,
                                 String os,
                                 String initialShortcut,
                                 ShortcutChangeListener listener) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.project = project;
        this.listener = listener;

        // Create modifier checkboxes
        ctrlCheckBox = new JCheckBox("Ctrl");
        altCheckBox = new JCheckBox("Alt");
        shiftCheckBox = new JCheckBox("Shift");
        metaCheckBox = new JCheckBox("Cmd");

        // For non-Mac OS, disable meta key
        if (!"Mac".equalsIgnoreCase(os)) {
            metaCheckBox.setEnabled(false);
        }

        // Create key dropdown
        String[] keys = getAvailableKeys();
        keyCombo = new JComboBox<>(keys);

        setupComponents(os);
        setupListeners();
        parseAndApplyInitialShortcut(initialShortcut);
    }

    private String[] getAvailableKeys() {
        return new String[]{
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
                "ENTER", "SPACE", "TAB"
        };
    }

    private void setupComponents(String os) {
        add(new JLabel(os + " Shortcut:"));
        add(ctrlCheckBox);
        add(altCheckBox);
        add(shiftCheckBox);
        add(metaCheckBox);
        add(keyCombo);
    }

    private void setupListeners() {
        ItemListener modifierListener = e -> updateShortcut();
        ctrlCheckBox.addItemListener(modifierListener);
        altCheckBox.addItemListener(modifierListener);
        shiftCheckBox.addItemListener(modifierListener);
        metaCheckBox.addItemListener(modifierListener);
        keyCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateShortcut();
            }
        });
    }

    private void parseAndApplyInitialShortcut(String shortcutStr) {
        if (shortcutStr == null || shortcutStr.isEmpty()) {
            return;
        }

        try {
            KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcutStr);
            if (keyStroke != null) {
                // Set modifiers
                int modifiers = keyStroke.getModifiers();
                ctrlCheckBox.setSelected((modifiers & InputEvent.CTRL_DOWN_MASK) != 0);
                altCheckBox.setSelected((modifiers & InputEvent.ALT_DOWN_MASK) != 0);
                shiftCheckBox.setSelected((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0);
                metaCheckBox.setSelected((modifiers & InputEvent.META_DOWN_MASK) != 0);

                // Get the key code and convert to string
                int keyCode = keyStroke.getKeyCode();
                String keyText = switch (keyCode) {
                    case KeyEvent.VK_ENTER -> "ENTER";
                    case KeyEvent.VK_SPACE -> "SPACE";
                    case KeyEvent.VK_TAB -> "TAB";
                    default -> String.valueOf((char) keyCode);
                };

                // Find and select the matching key in combo box
                for (int i = 0; i < keyCombo.getItemCount(); i++) {
                    if (keyCombo.getItemAt(i).equalsIgnoreCase(keyText)) {
                        keyCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            NotificationUtil.sendNotification(project, "Invalid shortcut: " + shortcutStr);
        }
    }

    private void updateShortcut() {
        int modifiers = 0;
        if (ctrlCheckBox.isSelected()) modifiers |= InputEvent.CTRL_DOWN_MASK;
        if (altCheckBox.isSelected()) modifiers |= InputEvent.ALT_DOWN_MASK;
        if (shiftCheckBox.isSelected()) modifiers |= InputEvent.SHIFT_DOWN_MASK;
        if (metaCheckBox.isSelected()) modifiers |= InputEvent.META_DOWN_MASK;

        String selectedKey = (String) keyCombo.getSelectedItem();
        if (selectedKey == null) return;

        int keyCode = getKeyCode(selectedKey);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        if (keyStroke != null) {
            listener.onShortcutChanged(keyStroke.toString());
        }
    }

    private int getKeyCode(@NotNull String keyName) {
        return switch (keyName.toUpperCase()) {
            case "ENTER" -> KeyEvent.VK_ENTER;
            case "SPACE" -> KeyEvent.VK_SPACE;
            case "TAB" -> KeyEvent.VK_TAB;
            default -> KeyEvent.getExtendedKeyCodeForChar(keyName.charAt(0));
        };
    }

    public String getCurrentShortcut() {
        int modifiers = 0;
        if (ctrlCheckBox.isSelected()) modifiers |= InputEvent.CTRL_DOWN_MASK;
        if (altCheckBox.isSelected()) modifiers |= InputEvent.ALT_DOWN_MASK;
        if (shiftCheckBox.isSelected()) modifiers |= InputEvent.SHIFT_DOWN_MASK;
        if (metaCheckBox.isSelected()) modifiers |= InputEvent.META_DOWN_MASK;

        String selectedKey = (String) keyCombo.getSelectedItem();
        if (selectedKey == null) return "";

        int keyCode = getKeyCode(selectedKey);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        return keyStroke != null ? keyStroke.toString() : "";
    }
}
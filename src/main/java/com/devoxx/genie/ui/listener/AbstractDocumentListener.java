package com.devoxx.genie.ui.listener;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class AbstractDocumentListener implements DocumentListener {
    @Override
    public void insertUpdate(DocumentEvent e) {
        // Abstract method - to be implemented by concrete subclasses
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        // Abstract method - to be implemented by concrete subclasses
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // Abstract method - to be implemented by concrete subclasses
    }
}

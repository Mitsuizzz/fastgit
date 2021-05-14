package com.mitsui.fastgit.ui;

import lombok.Data;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;
@Data
public class AutoCompleter implements KeyListener, ItemListener {
    private JComboBox owner = null;
    private JTextField editor = null;
    private ComboBoxModel model = null;

    public AutoCompleter(JComboBox comboBox) {
        this.owner = comboBox;
        this.editor = (JTextField)comboBox.getEditor().getEditorComponent();
        this.editor.addKeyListener(this);
        this.model = comboBox.getModel();
        this.owner.addItemListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    public ComboBoxModel getModel() {
        return this.model;
    }
    @Override

    public void keyReleased(KeyEvent e) {
        char ch = e.getKeyChar();
        if (ch != '\uffff' && !Character.isISOControl(ch) && ch != 127) {
            int caretPosition = this.editor.getCaretPosition();
            String str = this.editor.getText();
            if (str.length() != 0) {
                this.autoComplete(str, caretPosition);
            }
        }
    }

    protected void autoComplete(String strf, int caretPosition) {
        Object[] opts = this.getMatchingOptions(strf.substring(0, caretPosition));
        if (this.owner != null) {
            this.model = new DefaultComboBoxModel(opts);
            this.owner.setModel(this.model);
        }

        if (opts.length > 0) {
            String str = opts[0].toString();
            this.editor.setCaretPosition(caretPosition);
            if (this.owner != null) {
                try {
                    this.owner.showPopup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    protected Object[] getMatchingOptions(String str) {
        List v = new Vector();
        List v1 = new Vector();

        int k;
        for(k = 0; k < this.model.getSize(); ++k) {
            Object itemObj = this.model.getElementAt(k);
            if (itemObj != null) {
                String item = itemObj.toString().toLowerCase();
                if (item.startsWith(str.toLowerCase())) {
                    v.add(this.model.getElementAt(k));
                } else {
                    v1.add(this.model.getElementAt(k));
                }
            } else {
                v1.add(this.model.getElementAt(k));
            }
        }

        for(k = 0; k < v1.size(); ++k) {
            v.add(v1.get(k));
        }

        if (v.isEmpty()) {
            v.add(str);
        }

        return v.toArray();
    }

    @Override
    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == 1) {
            int caretPosition = this.editor.getCaretPosition();
            if (caretPosition != -1) {
                try {
                    this.editor.moveCaretPosition(caretPosition);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}

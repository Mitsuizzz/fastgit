package com.mitsui.fastgit.ui;

import javax.swing.*;
import java.util.List;
import java.util.Vector;

public class JAutoCompleteComboBox extends JComboBox {

    private AutoCompleter completer;

    public JAutoCompleteComboBox() {
        this.addCompleter();
    }

    public JAutoCompleteComboBox(ComboBoxModel cm) {
        super(cm);
        this.addCompleter();
    }

    public JAutoCompleteComboBox(Object[] items) {
        super(items);
        this.addCompleter();
    }

    public JAutoCompleteComboBox(List v) {
        super((Vector)v);
        this.addCompleter();
    }

    private void addCompleter() {
        this.setEditable(true);
        this.completer = new AutoCompleter(this);
    }

    public void autoComplete(String str) {
        this.completer.autoComplete(str, str.length());
    }

    public String getText() {
        return ((JTextField)this.getEditor().getEditorComponent()).getText();
    }

    public void setText(String text) {
        ((JTextField)this.getEditor().getEditorComponent()).setText(text);
    }

    public boolean containsItem(String itemString) {
        for(int i = 0; i < this.getModel().getSize(); ++i) {
            String _item = " " + this.getModel().getElementAt(i);
            if (_item.equals(itemString)) {
                return true;
            }
        }

        return false;
    }

}

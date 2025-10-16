package com.caven.redistool.entity;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class HashEntry {
    private final StringProperty field = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();

    public HashEntry(String field, String value) {
        setField(field);
        setValue(value);
    }

    public String getField() { return field.get(); }
    public void setField(String value) { this.field.set(value); }
    public StringProperty fieldProperty() { return field; }

    public String getValue() { return value.get(); }
    public void setValue(String value) { this.value.set(value); }
    public StringProperty valueProperty() { return value; }
}

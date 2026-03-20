package com.nova.mcart.entity.enums;

public enum AttributeType {
    TEXT,
    STRING,
    NUMBER,
    BOOLEAN,
    SELECT,        // dropdown
    MULTI_SELECT;// multiple options

    public boolean isSelectableType() {
        return this == SELECT || this == MULTI_SELECT;
    }

    public boolean supportsPredefinedValues() {
        return isSelectableType();
    }
}
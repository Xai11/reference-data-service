package com.example.referencedata.model;

public enum AttributeType {
    STRING("Строка"),
    NUMBER("Число"),
    BOOLEAN("Да/Нет"),
    DATE("Дата"),
    REFERENCE("Ссылка на справочник");

    private final String label;

    AttributeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

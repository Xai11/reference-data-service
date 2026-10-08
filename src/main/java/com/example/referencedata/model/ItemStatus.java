package com.example.referencedata.model;

import java.time.LocalDate;

/** Состояние элемента справочника на конкретную дату. */
public enum ItemStatus {
    ACTIVE("Активен"),
    INACTIVE("Неактивен"),
    NOT_STARTED("Ещё не действует"),
    EXPIRED("Истёк");

    private final String label;

    ItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ItemStatus of(DictionaryItem item, LocalDate date) {
        if (!item.isActive()) {
            return INACTIVE;
        }
        if (item.getValidFrom() != null && item.getValidFrom().isAfter(date)) {
            return NOT_STARTED;
        }
        if (item.getValidTo() != null && item.getValidTo().isBefore(date)) {
            return EXPIRED;
        }
        return ACTIVE;
    }
}

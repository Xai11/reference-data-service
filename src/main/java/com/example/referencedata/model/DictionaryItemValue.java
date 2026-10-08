package com.example.referencedata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Значение атрибута у конкретного элемента (типизированные колонки вместо одной строковой). */
@Entity
@Table(name = "dictionary_item_value")
public class DictionaryItemValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private DictionaryItem item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private DictionaryAttribute attribute;

    @Column(name = "value_string", length = 2000)
    private String valueString;

    @Column(name = "value_number", precision = 19, scale = 6)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_item_id")
    private DictionaryItem valueItem;

    /** Значение в виде Java-объекта согласно типу атрибута. */
    public Object rawValue() {
        return switch (attribute.getType()) {
            case STRING -> valueString;
            case NUMBER -> valueNumber;
            case BOOLEAN -> valueBoolean;
            case DATE -> valueDate;
            case REFERENCE -> valueItem;
        };
    }

    /** Текстовое представление для таблиц и PDF. */
    public String displayValue() {
        Object value = rawValue();
        if (value == null) {
            return "";
        }
        return switch (attribute.getType()) {
            case BOOLEAN -> Boolean.TRUE.equals(value) ? "Да" : "Нет";
            case NUMBER -> ((BigDecimal) value).stripTrailingZeros().toPlainString();
            case REFERENCE -> ((DictionaryItem) value).getName();
            default -> value.toString();
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DictionaryItem getItem() { return item; }
    public void setItem(DictionaryItem item) { this.item = item; }

    public DictionaryAttribute getAttribute() { return attribute; }
    public void setAttribute(DictionaryAttribute attribute) { this.attribute = attribute; }

    public String getValueString() { return valueString; }
    public void setValueString(String valueString) { this.valueString = valueString; }

    public BigDecimal getValueNumber() { return valueNumber; }
    public void setValueNumber(BigDecimal valueNumber) { this.valueNumber = valueNumber; }

    public Boolean getValueBoolean() { return valueBoolean; }
    public void setValueBoolean(Boolean valueBoolean) { this.valueBoolean = valueBoolean; }

    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }

    public DictionaryItem getValueItem() { return valueItem; }
    public void setValueItem(DictionaryItem valueItem) { this.valueItem = valueItem; }
}

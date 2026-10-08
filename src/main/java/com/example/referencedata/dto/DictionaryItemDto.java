package com.example.referencedata.dto;

import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.model.DictionaryItemValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Элемент справочника. {@code attributes} — значения по кодам атрибутов (для ссылок — код целевого элемента);
 * {@code level} заполняется только в режиме {@code tree=true}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DictionaryItemDto(
        Long id,
        Long dictionaryId,
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        int sortOrder,
        boolean active,
        Long parentId,
        LocalDate validFrom,
        LocalDate validTo,
        Map<String, Object> attributes,
        Integer level) {

    public static DictionaryItemDto from(DictionaryItem entity) {
        return from(entity, null);
    }

    public static DictionaryItemDto from(DictionaryItem entity, Integer level) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (DictionaryItemValue value : entity.getValues()) {
            Object raw = value.rawValue();
            if (raw == null) {
                continue;
            }
            attributes.put(value.getAttribute().getCode(),
                    raw instanceof DictionaryItem ref ? ref.getCode() : raw);
        }
        return new DictionaryItemDto(entity.getId(), entity.getDictionary().getId(), entity.getCode(),
                entity.getName(), entity.getDescription(), entity.getSortOrder(), entity.isActive(),
                entity.getParentId(), entity.getValidFrom(), entity.getValidTo(), attributes, level);
    }

    public DictionaryItem toEntity() {
        DictionaryItem entity = new DictionaryItem();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(description);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        return entity;
    }
}

package com.example.referencedata.dto;

import com.example.referencedata.model.DictionaryItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictionaryItemDto(
        Long id,
        Long dictionaryId,
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        int sortOrder,
        boolean active) {

    public static DictionaryItemDto from(DictionaryItem entity) {
        return new DictionaryItemDto(entity.getId(), entity.getDictionary().getId(), entity.getCode(),
                entity.getName(), entity.getDescription(), entity.getSortOrder(), entity.isActive());
    }

    public DictionaryItem toEntity() {
        DictionaryItem entity = new DictionaryItem();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(description);
        entity.setSortOrder(sortOrder);
        entity.setActive(active);
        return entity;
    }
}

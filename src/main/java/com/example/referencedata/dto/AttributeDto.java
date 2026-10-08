package com.example.referencedata.dto;

import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.DictionaryAttribute;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttributeDto(
        Long id,
        Long dictionaryId,
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull AttributeType type,
        boolean required,
        int sortOrder,
        Long refDictionaryId) {

    public static AttributeDto from(DictionaryAttribute entity) {
        return new AttributeDto(entity.getId(), entity.getDictionary().getId(), entity.getCode(), entity.getName(),
                entity.getType(), entity.isRequired(), entity.getSortOrder(),
                entity.getRefDictionary() == null ? null : entity.getRefDictionary().getId());
    }

    public DictionaryAttribute toEntity() {
        DictionaryAttribute entity = new DictionaryAttribute();
        entity.setCode(code);
        entity.setName(name);
        entity.setType(type);
        entity.setRequired(required);
        entity.setSortOrder(sortOrder);
        return entity;
    }
}

package com.example.referencedata.dto;

import com.example.referencedata.model.Dictionary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictionaryDto(
        Long id,
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        boolean active) {

    public static DictionaryDto from(Dictionary entity) {
        return new DictionaryDto(entity.getId(), entity.getCode(), entity.getName(),
                entity.getDescription(), entity.isActive());
    }

    public Dictionary toEntity() {
        Dictionary entity = new Dictionary();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(description);
        entity.setActive(active);
        return entity;
    }
}

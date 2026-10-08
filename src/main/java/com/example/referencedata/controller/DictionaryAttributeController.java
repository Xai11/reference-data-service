package com.example.referencedata.controller;

import com.example.referencedata.dto.AttributeDto;
import com.example.referencedata.service.DictionaryAttributeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DictionaryAttributeController {

    private final DictionaryAttributeService service;

    public DictionaryAttributeController(DictionaryAttributeService service) {
        this.service = service;
    }

    @GetMapping("/dictionaries/{dictionaryId}/attributes")
    public List<AttributeDto> list(@PathVariable Long dictionaryId) {
        return service.findByDictionary(dictionaryId).stream().map(AttributeDto::from).toList();
    }

    @PostMapping("/dictionaries/{dictionaryId}/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    public AttributeDto create(@PathVariable Long dictionaryId, @Valid @RequestBody AttributeDto dto) {
        return AttributeDto.from(service.create(dictionaryId, dto.toEntity(), dto.refDictionaryId()));
    }

    @GetMapping("/attributes/{id}")
    public AttributeDto get(@PathVariable Long id) {
        return AttributeDto.from(service.getById(id));
    }

    @PutMapping("/attributes/{id}")
    public AttributeDto update(@PathVariable Long id, @Valid @RequestBody AttributeDto dto) {
        return AttributeDto.from(service.update(id, dto.toEntity(), dto.refDictionaryId()));
    }

    @DeleteMapping("/attributes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

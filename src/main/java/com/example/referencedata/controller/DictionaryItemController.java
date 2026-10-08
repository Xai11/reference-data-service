package com.example.referencedata.controller;

import com.example.referencedata.dto.DictionaryItemDto;
import com.example.referencedata.service.DictionaryItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DictionaryItemController {

    private final DictionaryItemService service;

    public DictionaryItemController(DictionaryItemService service) {
        this.service = service;
    }

    @GetMapping("/dictionaries/{dictionaryId}/items")
    public List<DictionaryItemDto> list(@PathVariable Long dictionaryId,
                                        @RequestParam(defaultValue = "false") boolean onlyActive) {
        return service.findByDictionary(dictionaryId, onlyActive).stream()
                .map(DictionaryItemDto::from).toList();
    }

    @GetMapping("/dictionaries/{dictionaryId}/items/by-code/{code}")
    public DictionaryItemDto getByCode(@PathVariable Long dictionaryId, @PathVariable String code) {
        return DictionaryItemDto.from(service.getByCode(dictionaryId, code));
    }

    @PostMapping("/dictionaries/{dictionaryId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryItemDto create(@PathVariable Long dictionaryId,
                                    @Valid @RequestBody DictionaryItemDto dto) {
        return DictionaryItemDto.from(service.create(dictionaryId, dto.toEntity()));
    }

    @GetMapping("/items/{id}")
    public DictionaryItemDto get(@PathVariable Long id) {
        return DictionaryItemDto.from(service.getById(id));
    }

    @PutMapping("/items/{id}")
    public DictionaryItemDto update(@PathVariable Long id, @Valid @RequestBody DictionaryItemDto dto) {
        return DictionaryItemDto.from(service.update(id, dto.toEntity()));
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

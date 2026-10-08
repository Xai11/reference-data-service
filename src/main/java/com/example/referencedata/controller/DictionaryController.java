package com.example.referencedata.controller;

import com.example.referencedata.dto.DictionaryDto;
import com.example.referencedata.service.DictionaryService;
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
@RequestMapping("/api/dictionaries")
public class DictionaryController {

    private final DictionaryService service;

    public DictionaryController(DictionaryService service) {
        this.service = service;
    }

    @GetMapping
    public List<DictionaryDto> list(@RequestParam(required = false) String name,
                                    @RequestParam(defaultValue = "false") boolean onlyActive) {
        var result = name != null && !name.isBlank() ? service.searchByName(name)
                : onlyActive ? service.findActive() : service.findAll();
        return result.stream().map(DictionaryDto::from).toList();
    }

    @GetMapping("/{id}")
    public DictionaryDto get(@PathVariable Long id) {
        return DictionaryDto.from(service.getById(id));
    }

    @GetMapping("/by-code/{code}")
    public DictionaryDto getByCode(@PathVariable String code) {
        return DictionaryDto.from(service.getByCode(code));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryDto create(@Valid @RequestBody DictionaryDto dto) {
        return DictionaryDto.from(service.create(dto.toEntity()));
    }

    @PutMapping("/{id}")
    public DictionaryDto update(@PathVariable Long id, @Valid @RequestBody DictionaryDto dto) {
        return DictionaryDto.from(service.update(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

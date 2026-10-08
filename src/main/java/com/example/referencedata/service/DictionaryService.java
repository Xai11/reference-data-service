package com.example.referencedata.service;

import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.repository.DictionaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DictionaryService {

    private final DictionaryRepository repository;

    public DictionaryService(DictionaryRepository repository) {
        this.repository = repository;
    }

    public List<Dictionary> findAll() {
        return repository.findAll();
    }

    public List<Dictionary> findActive() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    public List<Dictionary> searchByName(String name) {
        return repository.findByNameContainingIgnoreCaseOrderByNameAsc(name);
    }

    public Dictionary getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Справочник не найден: id=" + id));
    }

    public Dictionary getByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Справочник не найден: code=" + code));
    }

    @Transactional
    public Dictionary create(Dictionary dictionary) {
        if (repository.existsByCode(dictionary.getCode())) {
            throw new DuplicateCodeException("Справочник с кодом уже существует: " + dictionary.getCode());
        }
        dictionary.setId(null);
        return repository.save(dictionary);
    }

    @Transactional
    public Dictionary update(Long id, Dictionary changes) {
        Dictionary existing = getById(id);
        if (!existing.getCode().equals(changes.getCode()) && repository.existsByCode(changes.getCode())) {
            throw new DuplicateCodeException("Справочник с кодом уже существует: " + changes.getCode());
        }
        existing.setCode(changes.getCode());
        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setActive(changes.isActive());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getById(id));
    }
}

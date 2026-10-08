package com.example.referencedata.service;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.repository.DictionaryAttributeRepository;
import com.example.referencedata.repository.DictionaryItemRepository;
import com.example.referencedata.repository.DictionaryItemValueRepository;
import com.example.referencedata.repository.DictionaryRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DictionaryService {

    private final DictionaryRepository repository;
    private final DictionaryItemRepository itemRepository;
    private final DictionaryItemValueRepository valueRepository;
    private final DictionaryAttributeRepository attributeRepository;

    public DictionaryService(DictionaryRepository repository, DictionaryItemRepository itemRepository,
                             DictionaryItemValueRepository valueRepository,
                             DictionaryAttributeRepository attributeRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.valueRepository = valueRepository;
        this.attributeRepository = attributeRepository;
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
    @PreAuthorize("hasRole('EDITOR')")
    public Dictionary create(Dictionary dictionary) {
        if (repository.existsByCode(dictionary.getCode())) {
            throw new DuplicateCodeException("Справочник с кодом уже существует: " + dictionary.getCode());
        }
        dictionary.setId(null);
        return repository.save(dictionary);
    }

    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public Dictionary update(Long id, Dictionary changes) {
        Dictionary existing = getById(id);
        if (!existing.getCode().equals(changes.getCode()) && repository.existsByCode(changes.getCode())) {
            throw new DuplicateCodeException("Справочник с кодом уже существует: " + changes.getCode());
        }
        if (existing.isHierarchical() && !changes.isHierarchical()
                && itemRepository.existsByDictionaryIdAndParentIsNotNull(id)) {
            throw new ConflictException("Нельзя отключить иерархию: в справочнике есть элементы с родителем");
        }
        existing.setCode(changes.getCode());
        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setActive(changes.isActive());
        existing.setHierarchical(changes.isHierarchical());
        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Справочник не найден: id=" + id);
        }
        if (valueRepository.existsReferencesFromOtherDictionaries(id)
                || attributeRepository.existsByRefDictionaryIdAndDictionaryIdNot(id, id)) {
            throw new ConflictException("Справочник используется в других справочниках (ссылки на его элементы)");
        }
        // Значения и связи «родитель» очищаются массово, чтобы порядок удаления строк не нарушал внешние ключи.
        valueRepository.deleteByDictionaryId(id);
        itemRepository.detachParents(id);
        repository.delete(getById(id));
    }
}

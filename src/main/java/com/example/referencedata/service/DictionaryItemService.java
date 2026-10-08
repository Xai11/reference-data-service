package com.example.referencedata.service;

import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.repository.DictionaryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DictionaryItemService {

    private final DictionaryItemRepository repository;
    private final DictionaryService dictionaryService;

    public DictionaryItemService(DictionaryItemRepository repository, DictionaryService dictionaryService) {
        this.repository = repository;
        this.dictionaryService = dictionaryService;
    }

    public List<DictionaryItem> findByDictionary(Long dictionaryId, boolean onlyActive) {
        dictionaryService.getById(dictionaryId);
        return onlyActive
                ? repository.findByDictionaryIdAndActiveTrueOrderBySortOrderAscNameAsc(dictionaryId)
                : repository.findByDictionaryIdOrderBySortOrderAscNameAsc(dictionaryId);
    }

    public DictionaryItem getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Элемент справочника не найден: id=" + id));
    }

    public DictionaryItem getByCode(Long dictionaryId, String code) {
        return repository.findByDictionaryIdAndCode(dictionaryId, code)
                .orElseThrow(() -> new NotFoundException(
                        "Элемент справочника не найден: dictionaryId=" + dictionaryId + ", code=" + code));
    }

    @Transactional
    public DictionaryItem create(Long dictionaryId, DictionaryItem item) {
        Dictionary dictionary = dictionaryService.getById(dictionaryId);
        if (repository.existsByDictionaryIdAndCode(dictionaryId, item.getCode())) {
            throw new DuplicateCodeException("Элемент с кодом уже существует в справочнике: " + item.getCode());
        }
        item.setId(null);
        item.setDictionary(dictionary);
        return repository.save(item);
    }

    @Transactional
    public DictionaryItem update(Long id, DictionaryItem changes) {
        DictionaryItem existing = getById(id);
        Long dictionaryId = existing.getDictionary().getId();
        if (!existing.getCode().equals(changes.getCode())
                && repository.existsByDictionaryIdAndCode(dictionaryId, changes.getCode())) {
            throw new DuplicateCodeException("Элемент с кодом уже существует в справочнике: " + changes.getCode());
        }
        existing.setCode(changes.getCode());
        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setSortOrder(changes.getSortOrder());
        existing.setActive(changes.isActive());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getById(id));
    }
}

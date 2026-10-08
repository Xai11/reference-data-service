package com.example.referencedata.service;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.InvalidDataException;
import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.repository.DictionaryAttributeRepository;
import com.example.referencedata.repository.DictionaryItemValueRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DictionaryAttributeService {

    private final DictionaryAttributeRepository repository;
    private final DictionaryItemValueRepository valueRepository;
    private final DictionaryService dictionaryService;

    public DictionaryAttributeService(DictionaryAttributeRepository repository,
                                      DictionaryItemValueRepository valueRepository,
                                      DictionaryService dictionaryService) {
        this.repository = repository;
        this.valueRepository = valueRepository;
        this.dictionaryService = dictionaryService;
    }

    public List<DictionaryAttribute> findByDictionary(Long dictionaryId) {
        dictionaryService.getById(dictionaryId);
        return repository.findByDictionaryIdOrderBySortOrderAscNameAsc(dictionaryId);
    }

    public DictionaryAttribute getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Атрибут не найден: id=" + id));
    }

    /** @param refDictionaryId справочник-цель для атрибута типа {@link AttributeType#REFERENCE} */
    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public DictionaryAttribute create(Long dictionaryId, DictionaryAttribute attribute, Long refDictionaryId) {
        Dictionary dictionary = dictionaryService.getById(dictionaryId);
        if (repository.existsByDictionaryIdAndCode(dictionaryId, attribute.getCode())) {
            throw new DuplicateCodeException("Атрибут с кодом уже существует: " + attribute.getCode());
        }
        attribute.setId(null);
        attribute.setDictionary(dictionary);
        attribute.setRefDictionary(resolveReference(attribute.getType(), refDictionaryId));
        return repository.save(attribute);
    }

    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public DictionaryAttribute update(Long id, DictionaryAttribute changes, Long refDictionaryId) {
        DictionaryAttribute existing = getById(id);
        Long dictionaryId = existing.getDictionary().getId();
        if (!existing.getCode().equals(changes.getCode())
                && repository.existsByDictionaryIdAndCode(dictionaryId, changes.getCode())) {
            throw new DuplicateCodeException("Атрибут с кодом уже существует: " + changes.getCode());
        }
        Dictionary newRef = resolveReference(changes.getType(), refDictionaryId);
        Long oldRefId = existing.getRefDictionary() == null ? null : existing.getRefDictionary().getId();
        boolean typeChanged = existing.getType() != changes.getType()
                || !java.util.Objects.equals(oldRefId, newRef == null ? null : newRef.getId());
        if (typeChanged && valueRepository.existsByAttributeId(id)) {
            throw new ConflictException("Нельзя изменить тип атрибута: у элементов уже есть его значения");
        }
        existing.setCode(changes.getCode());
        existing.setName(changes.getName());
        existing.setType(changes.getType());
        existing.setRequired(changes.isRequired());
        existing.setSortOrder(changes.getSortOrder());
        existing.setRefDictionary(newRef);
        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public void delete(Long id) {
        // Значения удаляются каскадом внешнего ключа вместе с атрибутом.
        repository.delete(getById(id));
    }

    private Dictionary resolveReference(AttributeType type, Long refDictionaryId) {
        if (type == AttributeType.REFERENCE) {
            if (refDictionaryId == null) {
                throw new InvalidDataException("Для атрибута-ссылки нужно указать справочник");
            }
            return dictionaryService.getById(refDictionaryId);
        }
        if (refDictionaryId != null) {
            throw new InvalidDataException("Справочник можно указывать только для атрибута-ссылки");
        }
        return null;
    }
}

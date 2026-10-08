package com.example.referencedata.service;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.DuplicateCodeException;
import com.example.referencedata.exception.InvalidDataException;
import com.example.referencedata.exception.NotFoundException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.model.DictionaryItemValue;
import com.example.referencedata.repository.DictionaryItemRepository;
import com.example.referencedata.repository.DictionaryItemValueRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DictionaryItemService {

    private static final int MAX_STRING_VALUE = 2000;

    private final DictionaryItemRepository repository;
    private final DictionaryItemValueRepository valueRepository;
    private final DictionaryService dictionaryService;

    public DictionaryItemService(DictionaryItemRepository repository,
                                 DictionaryItemValueRepository valueRepository,
                                 DictionaryService dictionaryService) {
        this.repository = repository;
        this.valueRepository = valueRepository;
        this.dictionaryService = dictionaryService;
    }

    public List<DictionaryItem> findByDictionary(Long dictionaryId, boolean onlyActive) {
        return findByDictionary(dictionaryId, onlyActive, null);
    }

    /** @param asOf если задана — только активные элементы, действующие на эту дату */
    public List<DictionaryItem> findByDictionary(Long dictionaryId, boolean onlyActive, LocalDate asOf) {
        dictionaryService.getById(dictionaryId);
        if (asOf != null) {
            return repository.findEffective(dictionaryId, asOf);
        }
        return onlyActive
                ? repository.findByDictionaryIdAndActiveTrueOrderBySortOrderAscNameAsc(dictionaryId)
                : repository.findByDictionaryIdOrderBySortOrderAscNameAsc(dictionaryId);
    }

    /** Элементы в порядке обхода дерева с уровнем вложенности. */
    public List<ItemTree.Node> findTree(Long dictionaryId, boolean onlyActive, LocalDate asOf) {
        return ItemTree.flatten(findByDictionary(dictionaryId, onlyActive, asOf));
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

    /**
     * @param parentId   родитель (только для иерархических справочников) или {@code null} для корня
     * @param attributes значения атрибутов по кодам; {@code null} трактуется как пустой набор
     */
    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public DictionaryItem create(Long dictionaryId, DictionaryItem item, Long parentId,
                                 Map<String, Object> attributes) {
        Dictionary dictionary = dictionaryService.getById(dictionaryId);
        if (repository.existsByDictionaryIdAndCode(dictionaryId, item.getCode())) {
            throw new DuplicateCodeException("Элемент с кодом уже существует в справочнике: " + item.getCode());
        }
        validatePeriod(item);
        item.setId(null);
        item.setDictionary(dictionary);
        item.setValues(new java.util.ArrayList<>());
        applyParent(item, dictionary, parentId);
        applyAttributes(item, dictionary, attributes == null ? Map.of() : attributes);
        return repository.save(item);
    }

    /**
     * Полная замена полей элемента и его родителя; значения атрибутов заменяются, если {@code attributes != null},
     * и остаются без изменений, если {@code attributes == null}.
     */
    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public DictionaryItem update(Long id, DictionaryItem changes, Long parentId, Map<String, Object> attributes) {
        DictionaryItem existing = getById(id);
        Dictionary dictionary = existing.getDictionary();
        if (!existing.getCode().equals(changes.getCode())
                && repository.existsByDictionaryIdAndCode(dictionary.getId(), changes.getCode())) {
            throw new DuplicateCodeException("Элемент с кодом уже существует в справочнике: " + changes.getCode());
        }
        validatePeriod(changes);
        existing.setCode(changes.getCode());
        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setSortOrder(changes.getSortOrder());
        existing.setActive(changes.isActive());
        existing.setValidFrom(changes.getValidFrom());
        existing.setValidTo(changes.getValidTo());
        applyParent(existing, dictionary, parentId);
        if (attributes != null) {
            applyAttributes(existing, dictionary, attributes);
        }
        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasRole('EDITOR')")
    public void delete(Long id) {
        DictionaryItem item = getById(id);
        if (repository.existsByParentId(id)) {
            throw new ConflictException("Нельзя удалить элемент: у него есть дочерние элементы");
        }
        if (valueRepository.existsByValueItemId(id)) {
            throw new ConflictException("Нельзя удалить элемент: на него ссылаются элементы других справочников");
        }
        repository.delete(item);
    }

    // --- Правила предметной области ---

    private void validatePeriod(DictionaryItem item) {
        if (item.getValidFrom() != null && item.getValidTo() != null
                && item.getValidFrom().isAfter(item.getValidTo())) {
            throw new InvalidDataException("Дата начала действия позже даты окончания");
        }
    }

    private void applyParent(DictionaryItem item, Dictionary dictionary, Long parentId) {
        if (parentId == null) {
            item.setParent(null);
            return;
        }
        if (!dictionary.isHierarchical()) {
            throw new InvalidDataException("Справочник не является иерархическим: родитель не допускается");
        }
        DictionaryItem parent = getById(parentId);
        if (!parent.getDictionary().getId().equals(dictionary.getId())) {
            throw new InvalidDataException("Родитель должен принадлежать тому же справочнику");
        }
        if (item.getId() != null) {
            for (DictionaryItem p = parent; p != null; p = p.getParent()) {
                if (item.getId().equals(p.getId())) {
                    throw new InvalidDataException(
                            "Нельзя сделать элемент потомком самого себя или своего потомка");
                }
            }
        }
        item.setParent(parent);
    }

    private void applyAttributes(DictionaryItem item, Dictionary dictionary, Map<String, Object> attributes) {
        Map<String, DictionaryAttribute> byCode = dictionary.getAttributes().stream()
                .collect(Collectors.toMap(DictionaryAttribute::getCode, Function.identity()));
        for (String code : attributes.keySet()) {
            if (!byCode.containsKey(code)) {
                throw new InvalidDataException("Неизвестный атрибут: " + code);
            }
        }
        for (DictionaryAttribute attribute : dictionary.getAttributes()) {
            Object raw = attributes.get(attribute.getCode());
            DictionaryItemValue existing = item.getValues().stream()
                    .filter(v -> Objects.equals(v.getAttribute().getId(), attribute.getId()))
                    .findFirst().orElse(null);

            boolean absent = raw == null || (raw instanceof String s && s.isBlank());
            if (absent) {
                if (attribute.isRequired()) {
                    throw new InvalidDataException("Атрибут «" + attribute.getName() + "» обязателен");
                }
                if (existing != null) {
                    item.getValues().remove(existing);
                }
                continue;
            }

            Object typed = coerce(attribute, raw);
            DictionaryItemValue value = existing;
            if (value == null) {
                value = new DictionaryItemValue();
                value.setItem(item);
                value.setAttribute(attribute);
                item.getValues().add(value);
            }
            assign(value, attribute, typed);
        }
    }

    private Object coerce(DictionaryAttribute attribute, Object raw) {
        String label = "Атрибут «" + attribute.getName() + "»: ";
        try {
            return switch (attribute.getType()) {
                case STRING -> {
                    String text = raw.toString();
                    if (text.length() > MAX_STRING_VALUE) {
                        throw new InvalidDataException(label + "значение длиннее " + MAX_STRING_VALUE + " символов");
                    }
                    yield text;
                }
                case NUMBER -> new BigDecimal(raw.toString().trim());
                case BOOLEAN -> {
                    if (raw instanceof Boolean b) {
                        yield b;
                    }
                    String text = raw.toString().trim();
                    if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
                        yield Boolean.valueOf(text);
                    }
                    throw new InvalidDataException(label + "ожидается true или false");
                }
                case DATE -> raw instanceof LocalDate d ? d : LocalDate.parse(raw.toString().trim());
                case REFERENCE -> repository
                        .findByDictionaryIdAndCode(attribute.getRefDictionary().getId(), raw.toString().trim())
                        .orElseThrow(() -> new InvalidDataException(label + "элемент с кодом «" + raw
                                + "» не найден в справочнике «" + attribute.getRefDictionary().getName() + "»"));
            };
        } catch (NumberFormatException e) {
            throw new InvalidDataException(label + "ожидается число");
        } catch (DateTimeParseException e) {
            throw new InvalidDataException(label + "ожидается дата в формате ГГГГ-ММ-ДД");
        }
    }

    private void assign(DictionaryItemValue value, DictionaryAttribute attribute, Object typed) {
        value.setValueString(null);
        value.setValueNumber(null);
        value.setValueBoolean(null);
        value.setValueDate(null);
        value.setValueItem(null);
        switch (attribute.getType()) {
            case STRING -> value.setValueString((String) typed);
            case NUMBER -> value.setValueNumber((BigDecimal) typed);
            case BOOLEAN -> value.setValueBoolean((Boolean) typed);
            case DATE -> value.setValueDate((LocalDate) typed);
            case REFERENCE -> value.setValueItem((DictionaryItem) typed);
        }
    }
}

package com.example.referencedata.service;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.InvalidDataException;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import com.example.referencedata.repository.DictionaryItemRepository;
import com.example.referencedata.repository.DictionaryItemValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DictionaryItemServiceTest {

    private final DictionaryItemRepository repository = Mockito.mock(DictionaryItemRepository.class);
    private final DictionaryItemValueRepository valueRepository = Mockito.mock(DictionaryItemValueRepository.class);
    private final DictionaryService dictionaryService = Mockito.mock(DictionaryService.class);
    private final DictionaryItemService service =
            new DictionaryItemService(repository, valueRepository, dictionaryService);

    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary();
        dictionary.setId(1L);
        dictionary.setCode("D");
        dictionary.setName("Dict");
        dictionary.setHierarchical(true);
        when(dictionaryService.getById(1L)).thenReturn(dictionary);
        when(repository.save(any(DictionaryItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private DictionaryItem item(Long id, String code, DictionaryItem parent) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setCode(code);
        item.setName(code);
        item.setDictionary(dictionary);
        item.setParent(parent);
        return item;
    }

    private DictionaryAttribute attribute(long id, String code, AttributeType type, boolean required) {
        DictionaryAttribute attribute = new DictionaryAttribute();
        attribute.setId(id);
        attribute.setCode(code);
        attribute.setName(code);
        attribute.setType(type);
        attribute.setRequired(required);
        attribute.setDictionary(dictionary);
        dictionary.getAttributes().add(attribute);
        return attribute;
    }

    @Test
    void rejectsMakingItemItsOwnDescendantParent() {
        DictionaryItem root = item(10L, "ROOT", null);
        DictionaryItem child = item(11L, "CHILD", root);
        when(repository.findById(11L)).thenReturn(Optional.of(child));
        when(repository.findById(10L)).thenReturn(Optional.of(root));

        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> service.update(10L, root, 11L, null));
        assertTrue(e.getMessage().contains("потомком"));
    }

    @Test
    void rejectsParentInNonHierarchicalDictionary() {
        dictionary.setHierarchical(false);
        DictionaryItem parent = item(10L, "P", null);
        when(repository.findById(10L)).thenReturn(Optional.of(parent));

        assertThrows(InvalidDataException.class,
                () -> service.create(1L, item(null, "NEW", null), 10L, null));
    }

    @Test
    void rejectsInvertedPeriod() {
        DictionaryItem item = item(null, "NEW", null);
        item.setValidFrom(LocalDate.of(2026, 5, 1));
        item.setValidTo(LocalDate.of(2026, 4, 1));

        assertThrows(InvalidDataException.class, () -> service.create(1L, item, null, null));
    }

    @Test
    void createsItemWithTypedAttributeValues() {
        attribute(1L, "weight", AttributeType.NUMBER, true);
        attribute(2L, "since", AttributeType.DATE, false);

        DictionaryItem saved = service.create(1L, item(null, "NEW", null), null,
                Map.of("weight", 12.5, "since", "2026-01-31"));

        assertEquals(2, saved.getValues().size());
        assertEquals(new BigDecimal("12.5"), saved.getValues().stream()
                .filter(v -> v.getAttribute().getCode().equals("weight")).findFirst().orElseThrow().getValueNumber());
        assertEquals(LocalDate.of(2026, 1, 31), saved.getValues().stream()
                .filter(v -> v.getAttribute().getCode().equals("since")).findFirst().orElseThrow().getValueDate());
    }

    @Test
    void requiredAttributeMustBeProvided() {
        attribute(1L, "weight", AttributeType.NUMBER, true);

        assertThrows(InvalidDataException.class, () -> service.create(1L, item(null, "NEW", null), null, null));
    }

    @Test
    void invalidNumberAndUnknownAttributeAreRejected() {
        attribute(1L, "weight", AttributeType.NUMBER, false);

        assertThrows(InvalidDataException.class,
                () -> service.create(1L, item(null, "A", null), null, Map.of("weight", "abc")));
        assertThrows(InvalidDataException.class,
                () -> service.create(1L, item(null, "B", null), null, Map.of("nope", "x")));
    }

    @Test
    void resolvesReferenceByCodeInTargetDictionary() {
        Dictionary countries = new Dictionary();
        countries.setId(2L);
        countries.setName("Страны");
        DictionaryAttribute country = attribute(1L, "country", AttributeType.REFERENCE, true);
        country.setRefDictionary(countries);
        DictionaryItem russia = item(50L, "RU", null);
        when(repository.findByDictionaryIdAndCode(2L, "RU")).thenReturn(Optional.of(russia));
        when(repository.findByDictionaryIdAndCode(2L, "XX")).thenReturn(Optional.empty());

        DictionaryItem saved = service.create(1L, item(null, "RUB", null), null, Map.of("country", "RU"));
        assertEquals(russia, saved.getValues().get(0).getValueItem());

        assertThrows(InvalidDataException.class,
                () -> service.create(1L, item(null, "XXX", null), null, Map.of("country", "XX")));
    }

    @Test
    void deleteIsBlockedByChildrenAndReferences() {
        DictionaryItem target = item(10L, "T", null);
        when(repository.findById(10L)).thenReturn(Optional.of(target));

        when(repository.existsByParentId(10L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.delete(10L));

        when(repository.existsByParentId(10L)).thenReturn(false);
        when(valueRepository.existsByValueItemId(10L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.delete(10L));

        when(valueRepository.existsByValueItemId(10L)).thenReturn(false);
        service.delete(10L);
        Mockito.verify(repository).delete(target);
    }

    @Test
    void effectiveListUsesAsOfQuery() {
        LocalDate date = LocalDate.of(2026, 6, 1);
        service.findByDictionary(1L, false, date);

        Mockito.verify(repository).findEffective(1L, date);
    }
}

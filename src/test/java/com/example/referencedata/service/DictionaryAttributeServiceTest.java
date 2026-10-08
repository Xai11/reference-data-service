package com.example.referencedata.service;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.exception.InvalidDataException;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.repository.DictionaryAttributeRepository;
import com.example.referencedata.repository.DictionaryItemValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DictionaryAttributeServiceTest {

    private final DictionaryAttributeRepository repository = Mockito.mock(DictionaryAttributeRepository.class);
    private final DictionaryItemValueRepository valueRepository = Mockito.mock(DictionaryItemValueRepository.class);
    private final DictionaryService dictionaryService = Mockito.mock(DictionaryService.class);
    private final DictionaryAttributeService service =
            new DictionaryAttributeService(repository, valueRepository, dictionaryService);

    private Dictionary dictionary;
    private Dictionary other;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary();
        dictionary.setId(1L);
        other = new Dictionary();
        other.setId(2L);
        when(dictionaryService.getById(1L)).thenReturn(dictionary);
        when(dictionaryService.getById(2L)).thenReturn(other);
        when(repository.save(any(DictionaryAttribute.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private DictionaryAttribute attribute(String code, AttributeType type) {
        DictionaryAttribute attribute = new DictionaryAttribute();
        attribute.setCode(code);
        attribute.setName(code);
        attribute.setType(type);
        return attribute;
    }

    @Test
    void referenceAttributeRequiresTargetDictionary() {
        assertThrows(InvalidDataException.class,
                () -> service.create(1L, attribute("ref", AttributeType.REFERENCE), null));
        assertThrows(InvalidDataException.class,
                () -> service.create(1L, attribute("str", AttributeType.STRING), 2L));
    }

    @Test
    void createsReferenceAttribute() {
        DictionaryAttribute saved = service.create(1L, attribute("ref", AttributeType.REFERENCE), 2L);

        assertEquals(other, saved.getRefDictionary());
        assertEquals(dictionary, saved.getDictionary());
    }

    @Test
    void typeCannotChangeWhenValuesExist() {
        DictionaryAttribute existing = attribute("a", AttributeType.STRING);
        existing.setId(7L);
        existing.setDictionary(dictionary);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(valueRepository.existsByAttributeId(7L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(7L, attribute("a", AttributeType.NUMBER), null));
        // без смены типа — разрешено
        service.update(7L, attribute("a", AttributeType.STRING), null);
    }
}

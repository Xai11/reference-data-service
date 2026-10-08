package com.example.referencedata.service;

import com.example.referencedata.exception.ConflictException;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.repository.DictionaryAttributeRepository;
import com.example.referencedata.repository.DictionaryItemRepository;
import com.example.referencedata.repository.DictionaryItemValueRepository;
import com.example.referencedata.repository.DictionaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictionaryServiceTest {

    private final DictionaryRepository repository = Mockito.mock(DictionaryRepository.class);
    private final DictionaryItemRepository itemRepository = Mockito.mock(DictionaryItemRepository.class);
    private final DictionaryItemValueRepository valueRepository = Mockito.mock(DictionaryItemValueRepository.class);
    private final DictionaryAttributeRepository attributeRepository = Mockito.mock(DictionaryAttributeRepository.class);
    private final DictionaryService service =
            new DictionaryService(repository, itemRepository, valueRepository, attributeRepository);

    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary();
        dictionary.setId(1L);
        dictionary.setCode("D");
        dictionary.setName("Dict");
        dictionary.setHierarchical(true);
        when(repository.findById(1L)).thenReturn(Optional.of(dictionary));
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.save(Mockito.any(Dictionary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void hierarchyCannotBeDisabledWhileItemsHaveParents() {
        when(itemRepository.existsByDictionaryIdAndParentIsNotNull(1L)).thenReturn(true);
        Dictionary changes = new Dictionary();
        changes.setCode("D");
        changes.setName("Dict");
        changes.setHierarchical(false);

        assertThrows(ConflictException.class, () -> service.update(1L, changes));
    }

    @Test
    void deleteIsBlockedWhenOtherDictionariesReferenceIt() {
        when(valueRepository.existsReferencesFromOtherDictionaries(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(repository, never()).delete(Mockito.any(Dictionary.class));
    }

    @Test
    void deleteClearsValuesAndParentsBeforeRemoving() {
        service.delete(1L);

        verify(valueRepository).deleteByDictionaryId(1L);
        verify(itemRepository).detachParents(1L);
        verify(repository).delete(dictionary);
    }
}

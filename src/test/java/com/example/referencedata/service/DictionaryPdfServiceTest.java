package com.example.referencedata.service;

import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryPdfServiceTest {

    private final DictionaryService dictionaryService = Mockito.mock(DictionaryService.class);
    private final DictionaryItemService itemService = Mockito.mock(DictionaryItemService.class);
    private final DictionaryPdfService pdfService = new DictionaryPdfService(dictionaryService, itemService);

    @Test
    void generatesPdfForDictionaryWithCyrillicItems() {
        Dictionary dictionary = dictionary(1L, "COUNTRY", "Страны");
        DictionaryItem item = new DictionaryItem();
        item.setCode("RU");
        item.setName("Россия");
        item.setDictionary(dictionary);
        Mockito.when(itemService.findByDictionary(1L, false)).thenReturn(List.of(item));

        byte[] pdf = pdfService.generate(dictionary);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        assertTrue(pdf.length > 1000);
    }

    @Test
    void generatesPdfForEmptyDictionaryAndEmptyList() {
        Dictionary dictionary = dictionary(2L, "EMPTY", "Пустой");
        Mockito.when(itemService.findByDictionary(2L, false)).thenReturn(List.of());

        assertTrue(pdfService.generate(dictionary).length > 0);
        assertTrue(pdfService.generateAll(List.of()).length > 0);
    }

    private static Dictionary dictionary(Long id, String code, String name) {
        Dictionary d = new Dictionary();
        d.setId(id);
        d.setCode(code);
        d.setName(name);
        return d;
    }
}

package com.example.referencedata.service;

import com.example.referencedata.AbstractIntegrationTest;
import com.example.referencedata.model.AttributeType;
import com.example.referencedata.model.Dictionary;
import com.example.referencedata.model.DictionaryAttribute;
import com.example.referencedata.model.DictionaryItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PDF для иерархического справочника с атрибутами формируется на реальных данных. */
class DictionaryPdfRenderingTest extends AbstractIntegrationTest {

    @Autowired
    DictionaryService dictionaries;
    @Autowired
    DictionaryAttributeService attributes;
    @Autowired
    DictionaryItemService items;
    @Autowired
    DictionaryPdfService pdf;

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("editor", "x", "ROLE_EDITOR"));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rendersTreeWithAttributesAndPeriod() throws Exception {
        Dictionary region = new Dictionary();
        region.setCode("PDF_REGION");
        region.setName("Регионы для PDF");
        region.setHierarchical(true);
        region = dictionaries.create(region);
        for (String[] a : new String[][]{{"population", "Население", "NUMBER"}, {"note", "Заметка", "STRING"}}) {
            DictionaryAttribute attribute = new DictionaryAttribute();
            attribute.setCode(a[0]);
            attribute.setName(a[1]);
            attribute.setType(AttributeType.valueOf(a[2]));
            attributes.create(region.getId(), attribute, null);
        }
        DictionaryItem root = new DictionaryItem();
        root.setCode("ROOT");
        root.setName("Корень");
        root = items.create(region.getId(), root, null, Map.of("population", 100, "note", "главный"));
        DictionaryItem child = new DictionaryItem();
        child.setCode("CHILD");
        child.setName("Потомок");
        child.setValidFrom(java.time.LocalDate.of(2026, 1, 1));
        items.create(region.getId(), child, root.getId(), Map.of("population", 5));

        byte[] bytes = pdf.generate(dictionaries.getById(region.getId()));

        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        assertTrue(bytes.length > 2000);
        // сохраняем для визуальной проверки
        Path out = Path.of(System.getProperty("pdf.out", "target/pdf-sample.pdf"));
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.write(out, bytes);
    }
}

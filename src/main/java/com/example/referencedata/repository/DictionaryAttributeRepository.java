package com.example.referencedata.repository;

import com.example.referencedata.model.DictionaryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictionaryAttributeRepository extends JpaRepository<DictionaryAttribute, Long> {

    List<DictionaryAttribute> findByDictionaryIdOrderBySortOrderAscNameAsc(Long dictionaryId);

    boolean existsByDictionaryIdAndCode(Long dictionaryId, String code);

    /** Есть ли атрибуты других справочников, ссылающиеся на данный справочник. */
    boolean existsByRefDictionaryIdAndDictionaryIdNot(Long refDictionaryId, Long dictionaryId);
}

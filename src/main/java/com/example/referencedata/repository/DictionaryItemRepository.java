package com.example.referencedata.repository;

import com.example.referencedata.model.DictionaryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    List<DictionaryItem> findByDictionaryIdOrderBySortOrderAscNameAsc(Long dictionaryId);

    List<DictionaryItem> findByDictionaryIdAndActiveTrueOrderBySortOrderAscNameAsc(Long dictionaryId);

    Optional<DictionaryItem> findByDictionaryIdAndCode(Long dictionaryId, String code);

    boolean existsByDictionaryIdAndCode(Long dictionaryId, String code);
}

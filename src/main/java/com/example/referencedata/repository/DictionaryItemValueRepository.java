package com.example.referencedata.repository;

import com.example.referencedata.model.DictionaryItemValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DictionaryItemValueRepository extends JpaRepository<DictionaryItemValue, Long> {

    boolean existsByAttributeId(Long attributeId);

    boolean existsByValueItemId(Long itemId);

    /** Есть ли ссылки на элементы справочника из значений элементов других справочников. */
    default boolean existsReferencesFromOtherDictionaries(Long dictionaryId) {
        return countReferencesFromOtherDictionaries(dictionaryId) > 0;
    }

    @Query("select count(v) from DictionaryItemValue v "
            + "where v.valueItem.dictionary.id = :dictionaryId and v.item.dictionary.id <> :dictionaryId")
    long countReferencesFromOtherDictionaries(@Param("dictionaryId") Long dictionaryId);

    @Modifying
    @Query("delete from DictionaryItemValue v where v.item.dictionary.id = :dictionaryId")
    int deleteByDictionaryId(@Param("dictionaryId") Long dictionaryId);
}

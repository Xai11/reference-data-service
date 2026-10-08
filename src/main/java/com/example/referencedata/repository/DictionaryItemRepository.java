package com.example.referencedata.repository;

import com.example.referencedata.model.DictionaryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    List<DictionaryItem> findByDictionaryIdOrderBySortOrderAscNameAsc(Long dictionaryId);

    List<DictionaryItem> findByDictionaryIdAndActiveTrueOrderBySortOrderAscNameAsc(Long dictionaryId);

    Optional<DictionaryItem> findByDictionaryIdAndCode(Long dictionaryId, String code);

    boolean existsByDictionaryIdAndCode(Long dictionaryId, String code);

    /** Активные элементы, действующие на указанную дату. */
    @Query("select i from DictionaryItem i where i.dictionary.id = :dictionaryId and i.active = true "
            + "and (i.validFrom is null or i.validFrom <= :asOf) and (i.validTo is null or i.validTo >= :asOf) "
            + "order by i.sortOrder asc, i.name asc")
    List<DictionaryItem> findEffective(@Param("dictionaryId") Long dictionaryId, @Param("asOf") LocalDate asOf);

    default boolean existsByParentId(Long parentId) {
        return countByParentId(parentId) > 0;
    }

    @Query("select count(i) from DictionaryItem i where i.parent.id = :parentId")
    long countByParentId(@Param("parentId") Long parentId);

    boolean existsByDictionaryIdAndParentIsNotNull(Long dictionaryId);

    @Modifying
    @Query("update DictionaryItem i set i.parent = null where i.dictionary.id = :dictionaryId")
    int detachParents(@Param("dictionaryId") Long dictionaryId);
}

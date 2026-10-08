package com.example.referencedata.repository;

import com.example.referencedata.model.Dictionary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DictionaryRepository extends JpaRepository<Dictionary, Long> {

    Optional<Dictionary> findByCode(String code);

    boolean existsByCode(String code);

    List<Dictionary> findByActiveTrueOrderByNameAsc();

    List<Dictionary> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}

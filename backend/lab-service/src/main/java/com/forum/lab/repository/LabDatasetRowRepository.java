package com.forum.lab.repository;

import com.forum.lab.document.LabDatasetRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabDatasetRowRepository extends MongoRepository<LabDatasetRow, String> {
    List<LabDatasetRow> findByProductIdOrderByOrderIndexAsc(String productId);
    Page<LabDatasetRow> findByProductIdOrderByOrderIndexAsc(String productId, Pageable pageable);
    long countByProductId(String productId);
    void deleteByProductId(String productId);
}

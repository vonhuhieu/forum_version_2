package com.forum.lab.repository;

import com.forum.lab.document.LabProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabProductRepository extends MongoRepository<LabProduct, String> {
    Optional<LabProduct> findByPublicId(String publicId);
    Optional<LabProduct> findBySessionId(String sessionId);
    Page<LabProduct> findByOwnerUsernameAndIsSavedTrue(String ownerUsername, Pageable pageable);
    long countByOwnerUsernameAndIsSavedTrue(String ownerUsername);
    void deleteByPublicId(String publicId);
}

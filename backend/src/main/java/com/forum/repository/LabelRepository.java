package com.forum.repository;

import com.forum.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findAllByOrderByIdDesc();
    java.util.Optional<Label> findByName(String name);
}

package com.forum.repository;

import com.forum.entity.CategoryGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, Long> {
    @EntityGraph(attributePaths = {"categories"})
    List<CategoryGroup> findAllByOrderByPositionOrderAsc();
}

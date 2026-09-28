package com.forum.repository;

import com.forum.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findAllByOrderByPositionOrderAsc();
    java.util.Optional<Menu> findByUrl(String url);
}

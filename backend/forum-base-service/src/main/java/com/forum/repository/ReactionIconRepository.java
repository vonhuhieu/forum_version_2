package com.forum.repository;

import com.forum.entity.ReactionIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReactionIconRepository extends JpaRepository<ReactionIcon, Long> {
    List<ReactionIcon> findAllByOrderByOrderIndexAscIdAsc();
}

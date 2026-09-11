package com.forum.repository;

import com.forum.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<UserFollow> findByFollowingId(Long followingId);

    Page<UserFollow> findByFollowerIdOrderByIdDesc(Long followerId, Pageable pageable);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}

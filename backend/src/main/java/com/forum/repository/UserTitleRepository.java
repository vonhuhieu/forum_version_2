package com.forum.repository;

import com.forum.entity.TitleType;
import com.forum.entity.UserTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTitleRepository extends JpaRepository<UserTitle, Long> {
    List<UserTitle> findByType(TitleType type);
    
    // Tìm các mốc điểm từ cao xuống thấp
    List<UserTitle> findByTypeOrderByMinPointsDesc(TitleType type);
    
    // Lấy title mặc định cho trạng thái chưa xác thực
    Optional<UserTitle> findFirstByType(TitleType type);
}

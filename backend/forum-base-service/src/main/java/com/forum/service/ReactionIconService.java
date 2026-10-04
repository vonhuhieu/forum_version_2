package com.forum.service;

import com.forum.dto.ReactionIconDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.ReactionIcon;
import com.forum.repository.ReactionIconRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReactionIconService {

    private final ReactionIconRepository reactionIconRepository;

    public ResponseDTO<List<ReactionIconDTO>> getAllIcons() {
        List<ReactionIconDTO> dtos = reactionIconRepository.findAllByOrderByOrderIndexAscIdAsc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtos);
    }

    public ResponseDTO<ReactionIconDTO> createIcon(ReactionIconDTO dto) {
        ReactionIcon iconEntity = new ReactionIcon();
        iconEntity.setIcon(dto.getIcon());
        iconEntity.setTooltip(dto.getTooltip());
        iconEntity.setColor(dto.getColor());
        iconEntity.setOrderIndex(dto.getOrderIndex());
        
        ReactionIcon saved = reactionIconRepository.save(iconEntity);
        return ResponseDTO.success(convertToDTO(saved));
    }

    public ResponseDTO<ReactionIconDTO> updateIcon(Long id, ReactionIconDTO dto) {
        return reactionIconRepository.findById(id).map(icon -> {
            icon.setIcon(dto.getIcon());
            icon.setTooltip(dto.getTooltip());
            icon.setColor(dto.getColor());
            icon.setOrderIndex(dto.getOrderIndex());
            return ResponseDTO.success(convertToDTO(reactionIconRepository.save(icon)));
        }).orElseThrow(() -> new RuntimeException("Reaction icon not found"));
    }

    public ResponseDTO<Void> deleteIcon(Long id) {
        reactionIconRepository.deleteById(id);
        return ResponseDTO.success(null);
    }

    public ReactionIconDTO convertToDTO(ReactionIcon icon) {
        if (icon == null) return null;
        ReactionIconDTO dto = new ReactionIconDTO();
        dto.setId(icon.getId());
        dto.setIcon(icon.getIcon());
        dto.setTooltip(icon.getTooltip());
        dto.setColor(icon.getColor());
        dto.setOrderIndex(icon.getOrderIndex());
        return dto;
    }
}

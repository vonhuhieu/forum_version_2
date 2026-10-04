package com.forum.service;

import com.forum.dto.LabelDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Label;
import com.forum.mapper.LabelMapper;
import com.forum.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LabelService {
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;
    private final com.forum.repository.ThreadRepository threadRepository;

    public ResponseDTO<List<LabelDTO>> getAllLabels() {
        return ResponseDTO.success(labelMapper.toDTOList(labelRepository.findAllByOrderByIdDesc()));
    }

    public ResponseDTO<LabelDTO> createLabel(LabelDTO dto) {
        Label label = labelMapper.toEntity(dto);
        return ResponseDTO.success(labelMapper.toDTO(labelRepository.save(label)));
    }

    public ResponseDTO<LabelDTO> updateLabel(Long id, LabelDTO dto) {
        return labelRepository.findById(id).map(label -> {
            label.setName(dto.getName());
            label.setColorCode(dto.getColorCode());
            label.setTextColor(dto.getTextColor());
            label.setBorderColor(dto.getBorderColor());
            label.setAdminOnly(dto.getAdminOnly() != null ? dto.getAdminOnly() : false);
            return ResponseDTO.success(labelMapper.toDTO(labelRepository.save(label)));
        }).orElseThrow(() -> new RuntimeException("Label not found"));
    }

    public ResponseDTO<Void> deleteLabel(Long id) {
        threadRepository.removeLabelFromThreads(id);
        labelRepository.deleteById(id);
        return ResponseDTO.success(null);
    }
}

package com.forum.mapper;

import com.forum.dto.LabelDTO;
import com.forum.entity.Label;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabelMapper {
    LabelDTO toDTO(Label label);
    Label toEntity(LabelDTO labelDTO);
    List<LabelDTO> toDTOList(List<Label> labels);
}

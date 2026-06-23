package com.forum.mapper;

import com.forum.dto.ThreadDTO;
import com.forum.entity.Thread;
import com.forum.dto.CategoryDTO;
import com.forum.entity.Category;
import com.forum.dto.UserDTO;
import com.forum.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LabelMapper.class, com.forum.service.PollService.class})
public interface ThreadMapper {
    ThreadMapper INSTANCE = Mappers.getMapper(ThreadMapper.class);

    ThreadDTO toDTO(Thread thread);
    
    Thread toEntity(ThreadDTO threadDTO);
    
    List<ThreadDTO> toDTOList(List<Thread> threads);

    @Mapping(target = "subCategories", ignore = true)
    CategoryDTO categoryToCategoryDTO(Category category);

    @Mapping(target = "roles", ignore = true)
    UserDTO userToUserDTO(User user);
}


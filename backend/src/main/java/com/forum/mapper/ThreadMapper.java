package com.forum.mapper;

import com.forum.dto.ThreadDTO;
import com.forum.entity.Thread;
import com.forum.dto.CategoryDTO;
import com.forum.entity.Category;
import com.forum.dto.UserDTO;
import com.forum.entity.User;
import com.forum.service.UserTitleService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LabelMapper.class, com.forum.service.PollService.class})
public abstract class ThreadMapper {
    public static final ThreadMapper INSTANCE = Mappers.getMapper(ThreadMapper.class);

    @Autowired
    protected UserTitleService userTitleService;

    public abstract ThreadDTO toDTO(Thread thread);
    
    public abstract Thread toEntity(ThreadDTO threadDTO);
    
    public abstract List<ThreadDTO> toDTOList(List<Thread> threads);

    @Mapping(target = "subCategories", ignore = true)
    public abstract CategoryDTO categoryToCategoryDTO(Category category);

    @Mapping(target = "roles", ignore = true)
    public abstract UserDTO userToUserDTO(User user);

    @AfterMapping
    protected void fillDisplayTitle(User user, @MappingTarget UserDTO dto) {
        if (user != null && dto != null && userTitleService != null) {
            dto.setDisplayTitle(userTitleService.resolveDisplayTitle(user, dto.getTrophyPoints()));
            dto.setIsVerifiedBadge(userTitleService.isVerifiedBadge(user, dto.getTrophyPoints()));
        }
    }
}

package com.forum.mapper;

import com.forum.dto.PostDTO;
import com.forum.dto.UserDTO;
import com.forum.entity.Post;
import com.forum.entity.User;
import com.forum.service.UserTitleService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class PostMapper {
    public static final PostMapper INSTANCE = Mappers.getMapper(PostMapper.class);

    @Autowired
    protected UserTitleService userTitleService;

    @Mapping(target = "threadId", source = "thread.id")
    public abstract PostDTO toDTO(Post post);

    @Mapping(target = "thread.id", source = "threadId")
    public abstract Post toEntity(PostDTO postDTO);

    public abstract List<PostDTO> toDTOList(List<Post> posts);

    @AfterMapping
    protected void fillUserDisplayTitle(User user, @MappingTarget UserDTO dto) {
        if (user != null && dto != null && userTitleService != null) {
            dto.setDisplayTitle(userTitleService.resolveDisplayTitle(user, dto.getTrophyPoints()));
            dto.setIsVerifiedBadge(userTitleService.isVerifiedBadge(user, dto.getTrophyPoints()));
        }
    }
}

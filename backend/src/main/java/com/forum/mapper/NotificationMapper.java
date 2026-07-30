package com.forum.mapper;

import com.forum.dto.NotificationDTO;
import com.forum.entity.Notification;
import com.forum.service.UserTitleService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class NotificationMapper {

    @Autowired
    protected UserTitleService userTitleService;

    @Mapping(source = "actor.id", target = "actorId")
    @Mapping(source = "actor.username", target = "actorUsername")
    @Mapping(source = "actor.displayName", target = "actorDisplayName")
    @Mapping(source = "actor.avatar", target = "actorAvatar")
    @Mapping(source = "thread.id", target = "threadId")
    @Mapping(source = "thread.title", target = "threadTitle")
    @Mapping(source = "thread.label.name", target = "threadLabelName")
    @Mapping(source = "thread.label.colorCode", target = "threadLabelColor")
    @Mapping(source = "thread.label.textColor", target = "threadLabelTextColor")
    @Mapping(source = "thread.label.borderColor", target = "threadLabelBorderColor")
    @Mapping(source = "post.id", target = "postId")
    public abstract NotificationDTO toDTO(Notification notification);

    public abstract List<NotificationDTO> toDTOList(List<Notification> notifications);

    @AfterMapping
    protected void fillActorVerifiedBadge(Notification notification, @MappingTarget NotificationDTO dto) {
        if (notification != null && notification.getActor() != null && dto != null && userTitleService != null) {
            dto.setActorIsVerifiedBadge(userTitleService.isVerifiedBadge(notification.getActor(), null));
        }
    }
}

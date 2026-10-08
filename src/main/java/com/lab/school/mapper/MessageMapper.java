package com.lab.school.mapper;

import com.lab.school.dto.message.InboxItemResponse;
import com.lab.school.dto.message.MessageResponse;
import com.lab.school.entity.Message;
import com.lab.school.entity.MessageRecipient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MessageMapper {

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.fullName")
    @Mapping(target = "recipientsCount", expression = "java(message.getRecipients().size())")
    MessageResponse toResponse(Message message);

    @Mapping(target = "messageId", source = "message.id")
    @Mapping(target = "senderId", source = "message.sender.id")
    @Mapping(target = "senderName", source = "message.sender.fullName")
    @Mapping(target = "subject", source = "message.subject")
    @Mapping(target = "body", source = "message.body")
    @Mapping(target = "type", source = "message.type")
    @Mapping(target = "createdAt", source = "message.createdAt")
    InboxItemResponse toInboxItem(MessageRecipient recipient);
}

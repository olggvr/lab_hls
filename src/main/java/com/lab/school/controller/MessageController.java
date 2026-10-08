package com.lab.school.controller;

import com.lab.school.dto.message.BroadcastRequest;
import com.lab.school.dto.message.DirectMessageRequest;
import com.lab.school.dto.message.InboxItemResponse;
import com.lab.school.dto.message.MessageResponse;
import com.lab.school.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.dto.common.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.dto.common.PaginationConstants.MAX_PAGE_SIZE;

@Tag(name = "Messages", description = "Сообщения и массовые рассылки")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Отправить личное сообщение одному или нескольким пользователям")
    @PostMapping
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody DirectMessageRequest request) {
        MessageResponse created = messageService.sendDirect(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @Operation(summary = "Массовая рассылка по классу / параллели / школе (одна транзакция)")
    @PostMapping("/broadcast")
    public ResponseEntity<MessageResponse> broadcast(@Valid @RequestBody BroadcastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.broadcast(request));
    }

    @Operation(summary = "Сообщение по id")
    @GetMapping("/{id}")
    public MessageResponse getById(@PathVariable Long id) {
        return messageService.getById(id);
    }

    @Operation(summary = "Входящие пользователя (постранично, не более 50)")
    @GetMapping("/inbox")
    public List<InboxItemResponse> inbox(
            @RequestParam @Positive Long userId,
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return messageService.inbox(userId, page, size).getContent();
    }

    @Operation(summary = "Отметить сообщение прочитанным")
    @PatchMapping("/{id}/read")
    public InboxItemResponse markRead(@PathVariable Long id, @RequestParam @Positive Long userId) {
        return messageService.markRead(id, userId);
    }

    @Operation(summary = "Удалить сообщение (вместе со списком получателей)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        messageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

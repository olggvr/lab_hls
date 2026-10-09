package com.lab.school.controller;

import com.lab.school.dto.message.BroadcastRequest;
import com.lab.school.dto.message.DirectMessageRequest;
import com.lab.school.dto.message.InboxItemResponse;
import com.lab.school.dto.message.MessageResponse;
import com.lab.school.service.MessageService;
import com.lab.school.utils.Locations;
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

import static com.lab.school.utils.PaginationConstants.DEFAULT_PAGE;
import static com.lab.school.utils.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.lab.school.utils.PaginationConstants.MAX_PAGE_SIZE;

@Tag(name = "Messages", description = "Сообщения и массовые рассылки")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Отправить личное сообщение одному или нескольким пользователям")
    @PostMapping
    public ResponseEntity<MessageResponse> sendDirectMessage(@Valid @RequestBody DirectMessageRequest request) {
        MessageResponse created = messageService.sendDirectMessage(request);
        return ResponseEntity.created(Locations.buildFor(created.id())).body(created);
    }

    @Operation(summary = "Массовая рассылка по классу / параллели / школе (одна транзакция)")
    @PostMapping("/broadcast")
    public ResponseEntity<MessageResponse> broadcastMessage(@Valid @RequestBody BroadcastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.broadcastMessage(request));
    }

    @Operation(summary = "Сообщение по id")
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMessage(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.getMessage(id));
    }

    @Operation(summary = "Входящие пользователя (постранично, не более 50)")
    @GetMapping("/inbox")
    public ResponseEntity<List<InboxItemResponse>> listInbox(
            @RequestParam @Positive Long userId,
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ResponseEntity.ok(messageService.listInbox(userId, page, size).getContent());
    }

    @Operation(summary = "Отметить сообщение прочитанным")
    @PatchMapping("/{id}/read")
    public ResponseEntity<InboxItemResponse> markMessageRead(@PathVariable Long id,
                                                             @RequestParam @Positive Long userId) {
        return ResponseEntity.ok(messageService.markMessageRead(id, userId));
    }

    @Operation(summary = "Удалить сообщение (вместе со списком получателей)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}

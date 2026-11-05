package com.lab.message.api;

import com.lab.message.service.ReactiveMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {
    private final ReactiveMessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageResponse> sendDirect(@Valid @RequestBody DirectMessageRequest request) {
        return messageService.sendDirect(request);
    }

    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageResponse> broadcast(@Valid @RequestBody BroadcastRequest request) {
        return messageService.broadcast(request);
    }

    @GetMapping("/{id}")
    public Mono<MessageResponse> getMessage(@PathVariable @Positive Long id) {
        return messageService.getMessage(id);
    }

    @GetMapping("/inbox")
    public Flux<InboxItemResponse> listInbox(
            @RequestParam @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return messageService.listInbox(userId, page, size);
    }

    @PatchMapping("/{id}/read")
    public Mono<InboxItemResponse> markRead(
            @PathVariable @Positive Long id, @RequestParam @Positive Long userId) {
        return messageService.markRead(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMessage(@PathVariable @Positive Long id) {
        return messageService.deleteMessage(id);
    }
}

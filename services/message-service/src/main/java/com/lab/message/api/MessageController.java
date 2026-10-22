package com.lab.message.api;

import com.lab.message.service.ReactiveMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController {
    private final ReactiveMessageService messageService;

    @PostMapping
    public Mono<ResponseEntity<MessageResponse>> sendDirect(@Valid @RequestBody DirectMessageRequest request) {
        return messageService.sendDirect(request)
                .map(body -> ResponseEntity.created(URI.create("./messages/" + body.id())).body(body));
    }

    @PostMapping("/broadcast")
    public Mono<ResponseEntity<MessageResponse>> broadcast(@Valid @RequestBody BroadcastRequest request) {
        return messageService.broadcast(request).map(body -> ResponseEntity.status(201).body(body));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<MessageResponse>> getMessage(@PathVariable Long id) {
        return messageService.getMessage(id).map(ResponseEntity::ok);
    }

    @GetMapping("/inbox")
    public Flux<InboxItemResponse> listInbox(
            @RequestParam @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return messageService.listInbox(userId, page, size);
    }

    @PatchMapping("/{id}/read")
    public Mono<ResponseEntity<InboxItemResponse>> markRead(
            @PathVariable Long id, @RequestParam @Positive Long userId) {
        return messageService.markRead(id, userId).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable Long id) {
        return messageService.deleteMessage(id).thenReturn(ResponseEntity.noContent().build());
    }
}

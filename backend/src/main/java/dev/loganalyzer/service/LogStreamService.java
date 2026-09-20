package dev.loganalyzer.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.loganalyzer.dto.LiveLogEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class LogStreamService {
    private final Map<UUID, SseEmitter> clients = new ConcurrentHashMap<>();

    public SseEmitter subscribe() {
        UUID clientId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(0L);
        clients.put(clientId, emitter);
        emitter.onCompletion(() -> clients.remove(clientId));
        emitter.onTimeout(() -> remove(clientId, emitter));
        emitter.onError(exception -> clients.remove(clientId));
        try {
            emitter.send(SseEmitter.event().name("status").reconnectTime(2_000)
                    .data(Map.of("status", "connected", "timestamp", Instant.now())));
        } catch (IOException exception) {
            remove(clientId, emitter);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(LiveLogEvent event) {
        clients.forEach((clientId, emitter) -> send(clientId, emitter,
                SseEmitter.event().id(event.id().toString()).name("log").data(event)));
    }

    @Scheduled(fixedRate = 15_000)
    void heartbeat() {
        clients.forEach((clientId, emitter) -> send(clientId, emitter,
                SseEmitter.event().comment("heartbeat")));
    }

    private void send(UUID clientId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            remove(clientId, emitter);
        }
    }

    private void remove(UUID clientId, SseEmitter emitter) {
        clients.remove(clientId);
        emitter.complete();
    }
}
package com.ktb.community.chat.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 세션별 구독 방 목록과 outbound sink를 관리한다.
 * 인메모리라 단일 인스턴스/스티키 세션 가정.
 */
@Component
public class SessionRegistry {

    public static class SessionConnection {
        private final String sessionId;
        private final Set<Long> roomIds;
        private final Sinks.Many<String> sink;

        SessionConnection(String sessionId, Set<Long> roomIds, Sinks.Many<String> sink) {
            this.sessionId = sessionId;
            this.roomIds = roomIds;
            this.sink = sink;
        }

        public String getSessionId() {
            return sessionId;
        }

        public Set<Long> getRoomIds() {
            return roomIds;
        }

        public Sinks.Many<String> getSink() {
            return sink;
        }
    }

    private final Map<String, SessionConnection> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> roomSubscriptions = new ConcurrentHashMap<>();

    public SessionConnection registerSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> {
            Set<Long> rooms = Collections.newSetFromMap(new ConcurrentHashMap<>());
            // multicase sink: 여러 subscriber(세션 send) 가능, backpressure buffer
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            return new SessionConnection(id, rooms, sink);
        });
    }

    public void removeSession(String sessionId) {
        SessionConnection connection = sessions.remove(sessionId);
        if (connection == null) {
            return;
        }
        // 구독 해제
        connection.getRoomIds().forEach(roomId -> {
            Set<String> sessionIds = roomSubscriptions.get(roomId);
            if (sessionIds != null) {
                sessionIds.remove(sessionId);
                if (sessionIds.isEmpty()) {
                    roomSubscriptions.remove(roomId);
                }
            }
        });
        connection.getSink().tryEmitComplete();
    }

    public void subscribe(String sessionId, Long roomId) {
        SessionConnection connection = registerSession(sessionId);
        connection.getRoomIds().add(roomId);
        roomSubscriptions
                .computeIfAbsent(roomId, id -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(sessionId);
    }

    public void unsubscribe(String sessionId, Long roomId) {
        SessionConnection connection = sessions.get(sessionId);
        if (connection != null) {
            connection.getRoomIds().remove(roomId);
        }
        Set<String> sessionIds = roomSubscriptions.get(roomId);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                roomSubscriptions.remove(roomId);
            }
        }
    }

    public void broadcast(Long roomId, String payload) {
        Set<String> sessionIds = roomSubscriptions.get(roomId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        sessionIds.forEach(sessionId -> {
            SessionConnection connection = sessions.get(sessionId);
            if (connection != null) {
                connection.getSink().tryEmitNext(payload);
            }
        });
    }
}

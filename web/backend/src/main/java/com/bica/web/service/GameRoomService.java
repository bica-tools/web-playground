package com.bica.web.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomService {

    public record GameRoom(
            String roomId,
            String typeString,
            String player1,
            String player2,
            List<String> moves,
            int currentState,
            String currentTurn,
            boolean finished,
            Instant created
    ) {}

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public GameRoom createRoom(String typeString, String playerName) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        var room = new GameRoom(roomId, typeString, playerName, null,
                new ArrayList<>(), -1, playerName, false, Instant.now());
        rooms.put(roomId, room);
        cleanup();
        return room;
    }

    public GameRoom joinRoom(String roomId, String playerName) {
        var room = rooms.get(roomId);
        if (room == null) return null;
        if (room.player2() != null) return room; // already full
        var updated = new GameRoom(room.roomId(), room.typeString(),
                room.player1(), playerName, room.moves(), room.currentState(),
                room.currentTurn(), false, room.created());
        rooms.put(roomId, updated);
        return updated;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public GameRoom makeMove(String roomId, String playerName, String method) {
        var room = rooms.get(roomId);
        if (room == null || room.finished()) return room;
        if (!playerName.equals(room.currentTurn())) return room;

        var newMoves = new ArrayList<>(room.moves());
        newMoves.add(method);

        String nextTurn = playerName.equals(room.player1()) ? room.player2() : room.player1();
        var updated = new GameRoom(room.roomId(), room.typeString(),
                room.player1(), room.player2(), newMoves, room.currentState(),
                nextTurn, false, room.created());
        rooms.put(roomId, updated);
        return updated;
    }

    private void cleanup() {
        var cutoff = Instant.now().minusSeconds(3600);
        rooms.entrySet().removeIf(e -> e.getValue().created().isBefore(cutoff));
    }
}

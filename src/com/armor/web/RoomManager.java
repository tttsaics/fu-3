package com.armor.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager {
    private static final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public static GameRoom createRoom(String hostName, String sessionId) {
        String code = generateRoomCode();
        GameRoom room = new GameRoom(code, hostName, sessionId);
        rooms.put(code, room);
        return room;
    }

    public static GameRoom createComputerRoom(String hostName, String sessionId) {
        String code = generateRoomCode();
        GameRoom room = new GameRoom(code, hostName, sessionId, true);
        rooms.put(code, room);
        return room;
    }

    public static GameRoom getRoom(String roomCode) {
        if (roomCode == null) {
            return null;
        }
        return rooms.get(roomCode.trim().toUpperCase());
    }

    public static boolean removeRoom(String roomCode) {
        return rooms.remove(roomCode) != null;
    }

    public static GameRoom joinRoom(String roomCode, String guestName, String sessionId) {
        GameRoom room = getRoom(roomCode);
        if (room == null) {
            return null;
        }
        if (!room.addGuest(guestName, sessionId)) {
            return null;
        }
        return room;
    }

    private static String generateRoomCode() {
        String code;
        do {
            code = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        } while (rooms.containsKey(code));
        return code;
    }
}

package com.armor.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager {
    private static final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public static GameRoom createRoom(String hostName, String sessionId) {
        String code = generateRoomCode();
        GameRoom room = new GameRoom(code, hostName, sessionId);
        rooms.put(code.toUpperCase(), room);
        return room;
    }

    public static GameRoom createComputerRoom(String hostName, String sessionId) {
        String code = generateRoomCode();
        GameRoom room = new GameRoom(code, hostName, sessionId, true);
        rooms.put(code.toUpperCase(), room);
        return room;
    }

    public static GameRoom getRoom(String roomCode) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            return null;
        }
        String normalized = roomCode.trim().toUpperCase();
        
        // 修正：如果輸入的是純數字且長度不足 4 位，自動補零 (例如 "1" -> "0001")
        if (normalized.matches("\\d+") && normalized.length() < 4) {
            normalized = String.format("%04d", Integer.parseInt(normalized));
        }
        
        return rooms.get(normalized);
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

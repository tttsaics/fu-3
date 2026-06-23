package com.armor.web;

import com.armor.enums.Action;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/game")
public class GameServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("state".equals(action)) {
            sendGameState(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支援的請求方式");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        if ("create".equals(action)) {
            createRoom(req, resp);
        } else if ("join".equals(action)) {
            joinRoom(req, resp);
        } else if ("create_computer".equals(action)) {
            createComputerRoom(req, resp);
        } else if ("submit".equals(action)) {
            submitAction(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支援的請求動作");
        }
    }

    private void createRoom(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String name = trimValue(req.getParameter("name"));
        if (name.isEmpty()) {
            forwardError(req, resp, "請輸入玩家名稱。");
            return;
        }
        GameRoom room = RoomManager.createRoom(name, req.getSession().getId());
        HttpSession session = req.getSession();
        session.setAttribute("roomCode", room.getCode());
        session.setAttribute("playerIndex", 1);
        resp.sendRedirect("game.jsp");
    }

    private void joinRoom(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String name = trimValue(req.getParameter("name"));
        String roomCode = trimValue(req.getParameter("roomCode"));
        if (name.isEmpty() || roomCode.isEmpty()) {
            forwardError(req, resp, "請輸入玩家名稱與房間代碼。");
            return;
        }
        GameRoom room = RoomManager.joinRoom(roomCode, name, req.getSession().getId());
        if (room == null) {
            forwardError(req, resp, "無效的房間代碼或房間已滿。請確認後重試。");
            return;
        }
        HttpSession session = req.getSession();
        session.setAttribute("roomCode", room.getCode());
        session.setAttribute("playerIndex", 2);
        resp.sendRedirect("game.jsp");
    }

    private void createComputerRoom(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String name = trimValue(req.getParameter("name"));
        if (name.isEmpty()) {
            forwardError(req, resp, "請輸入玩家名稱。", "index.jsp");
            return;
        }
        GameRoom room = RoomManager.createComputerRoom(name, req.getSession().getId());
        HttpSession session = req.getSession();
        session.setAttribute("roomCode", room.getCode());
        session.setAttribute("playerIndex", 1);
        resp.sendRedirect("game.jsp");
    }

    private void submitAction(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String roomCode = (String) session.getAttribute("roomCode");
        if (roomCode == null) {
            sendJsonError(resp, "尚未進入房間，請重新建立或加入房間。", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        GameRoom room = RoomManager.getRoom(roomCode);
        if (room == null) {
            sendJsonError(resp, "找不到房間，請重新建立或加入房間。", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String actionValue = req.getParameter("playerAction");
        if (actionValue == null || actionValue.isEmpty()) {
            sendJsonError(resp, "請選擇一個動作。", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Action action = parseAction(actionValue);
        if (action == null) {
            sendJsonError(resp, "未知的動作：" + escapeJson(actionValue), HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean accepted = room.submitAction(session.getId(), action);
        if (!accepted) {
            sendJsonError(resp, "當前無法送出動作，請稍候或重新整理頁面。", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        sendJsonResponse(resp, room, session.getId());
    }

    private void sendGameState(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String roomCode = (String) session.getAttribute("roomCode");
        if (roomCode == null) {
            sendJsonError(resp, "尚未加入房間。", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        GameRoom room = RoomManager.getRoom(roomCode);
        if (room == null) {
            sendJsonError(resp, "房間不存在或已被刪除。", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        sendJsonResponse(resp, room, session.getId());
    }

    private void sendJsonResponse(HttpServletResponse resp, GameRoom room, String sessionId) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(buildJsonForRoom(room, sessionId));
        out.flush();
    }

    private void sendJsonError(HttpServletResponse resp, String message, int status) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}");
        out.flush();
    }

    private String buildJsonForRoom(GameRoom room, String sessionId) {
        GameRoom.GameRoomStatus status = room.getStatusForSession(sessionId);
        if (status == null) {
            return "{\"success\":false,\"message\":\"無法讀取房間狀態。\"}";
        }
        return "{"
                + "\"success\":true,"
                + "\"roomCode\":\"" + escapeJson(status.roomCode) + "\"," 
                + "\"selfName\":\"" + escapeJson(status.selfName) + "\"," 
                + "\"opponentName\":\"" + escapeJson(status.opponentName) + "\"," 
                + "\"selfHp\":" + status.selfHp + ","
                + "\"selfMaxHp\":" + status.selfMaxHp + ","
                + "\"opponentHp\":" + status.opponentHp + ","
                + "\"opponentMaxHp\":" + status.opponentMaxHp + ","
                + "\"roundCount\":" + status.roundCount + ","
                + "\"guestPresent\":" + status.guestPresent + ","
                + "\"gameOver\":" + status.gameOver + ","
                + "\"canSubmit\":" + status.canSubmit + ","
                + "\"selfSubmitted\":" + status.selfSubmitted + ","
                + "\"opponentSubmitted\":" + status.opponentSubmitted + ","
                + "\"selfImage\":\"" + escapeJson(status.selfImage) + "\"," 
                + "\"opponentImage\":\"" + escapeJson(status.opponentImage) + "\"," 
                + "\"lastMessage\":\"" + escapeJson(status.lastMessage) + "\"," 
                + "\"roomStage\":\"" + escapeJson(status.roomStage) + "\"}"
                ;
    }

    private Action parseAction(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Action.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void forwardError(HttpServletRequest req, HttpServletResponse resp, String message) throws ServletException, IOException {
        req.setAttribute("message", message);
        RequestDispatcher dispatcher = req.getRequestDispatcher("error.jsp");
        dispatcher.forward(req, resp);
    }

    private void forwardError(HttpServletRequest req, HttpServletResponse resp, String message, String targetPage) throws ServletException, IOException {
        req.setAttribute("message", message);
        RequestDispatcher dispatcher = req.getRequestDispatcher(targetPage);
        dispatcher.forward(req, resp);
    }

    private String trimValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

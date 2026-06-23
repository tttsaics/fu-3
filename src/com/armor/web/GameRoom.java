package com.armor.web;

import com.armor.audio.AudioManager;
import com.armor.enums.Action;
import com.armor.logic.CombatManager;
import java.util.Objects;

public class GameRoom {
    private final String code;
    private final PlayerState host;
    private PlayerState guest;
    private final boolean computerRoom;
    private int roundCount;
    private boolean gameOver;
    private String lastMessage;

    public GameRoom(String code, String hostName, String sessionId) {
        this(code, hostName, sessionId, false);
    }

    public GameRoom(String code, String hostName, String sessionId, boolean computerRoom) {
        this.code = code;
        this.host = new PlayerState(hostName, sessionId);
        this.computerRoom = computerRoom;
        if (computerRoom) {
            this.guest = new PlayerState("電腦", "COMPUTER_SESSION", true);
            this.lastMessage = "已開始電腦對戰，請選擇你的動作。";
        } else {
            this.guest = null;
            this.lastMessage = "房間已建立，等待對手加入。";
        }
        this.roundCount = 1;
        this.gameOver = false;
    }

    public String getCode() {
        return code;
    }

    public PlayerState getHost() {
        return host;
    }

    public PlayerState getGuest() {
        return guest;
    }

    public int getRoundCount() {
        return roundCount;
    }

    public boolean isGuestPresent() {
        return guest != null;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean addGuest(String guestName, String sessionId) {
        if (this.guest != null) {
            return false;
        }
        this.guest = new PlayerState(guestName, sessionId);
        this.lastMessage = "對手已加入，開始對戰！";
        return true;
    }

    public boolean submitAction(String sessionId, Action action) {
        if (gameOver) {
            return false;
        }
        PlayerState player = findPlayer(sessionId);
        if (player == null || player.hasSubmitted()) {
            return false;
        }
        player.setAction(action == null ? Action.NONE : action);

        if (computerRoom && guest != null && guest.isComputer() && !guest.hasSubmitted()) {
            guest.setAction(new CombatManager(host.getAvatar(), guest.getAvatar(), new AudioManager()).enemyAI());
        }
        if (host.hasSubmitted() && guest != null && guest.hasSubmitted()) {
            resolveRound();
        }
        return true;
    }

    public GameRoomStatus getStatusForSession(String sessionId) {
        PlayerState self = findPlayer(sessionId);
        if (self == null) {
            return null;
        }
        PlayerState opponent = findOpponent(sessionId);
        return new GameRoomStatus(this, self, opponent);
    }

    private PlayerState findPlayer(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        if (Objects.equals(host.getSessionId(), sessionId)) {
            return host;
        }
        if (guest != null && Objects.equals(guest.getSessionId(), sessionId)) {
            return guest;
        }
        return null;
    }

    private PlayerState findOpponent(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        if (Objects.equals(host.getSessionId(), sessionId)) {
            return guest;
        }
        if (guest != null && Objects.equals(guest.getSessionId(), sessionId)) {
            return host;
        }
        return null;
    }

    private void resolveRound() {
        CombatManager combat = new CombatManager(host.getAvatar(), guest.getAvatar(), new AudioManager());
        Action hostAction = host.getAction();
        Action guestAction = guest.getAction();
        combat.resolveTurn(hostAction, guestAction);

        String roundSummary = buildRoundSummary(hostAction, guestAction);

        host.getAvatar().updateVuln();
        guest.getAvatar().updateVuln();
        host.getAvatar().nextVuln = 0;
        guest.getAvatar().nextVuln = 0;

        if (!host.getAvatar().isStunned) {
            host.getAvatar().resetMultipliers();
        }
        if (!guest.getAvatar().isStunned) {
            guest.getAvatar().resetMultipliers();
        }

        if (host.getAvatar().hp <= 0 || guest.getAvatar().hp <= 0 || roundCount >= 50) {
            gameOver = true;
            if (host.getAvatar().hp > guest.getAvatar().hp) {
                lastMessage = host.getName() + " 勝利！ " + roundSummary;
                host.getAvatar().state = "win";
                guest.getAvatar().state = "lose";
            } else if (guest.getAvatar().hp > host.getAvatar().hp) {
                lastMessage = guest.getName() + " 勝利！ " + roundSummary;
                host.getAvatar().state = "lose";
                guest.getAvatar().state = "win";
            } else {
                lastMessage = "平手！ " + roundSummary;
            }
        } else {
            lastMessage = roundSummary;
            roundCount++;
            host.resetAction();
            guest.resetAction();
        }
    }

    private String buildRoundSummary(Action hostAction, Action guestAction) {
        String hostResult = formatAction(hostAction);
        String guestResult = formatAction(guestAction);
        return String.format("第 %d 回合：%s 使出 %s，%s 使出 %s。%s: %d / %s: %d。",
                roundCount,
                host.getName(), hostResult,
                guest.getName(), guestResult,
                host.getName(), host.getAvatar().hp,
                guest.getName(), guest.getAvatar().hp);
    }

    private String formatAction(Action action) {
        if (action == null || action == Action.NONE) {
            return "無動作";
        }
        switch (action) {
            case L_ATK:
                return "輕攻擊";
            case M_ATK:
                return "中攻擊";
            case H_ATK:
                return "強攻擊";
            case GUARD:
                return "防禦";
            case PARRY_L:
                return "格檔(輕)";
            case PARRY_M:
                return "格檔(中)";
            case PARRY_H:
                return "格檔(強)";
            default:
                return action.name();
        }
    }

    public static class GameRoomStatus {
        public final String roomCode;
        public final String selfName;
        public final String opponentName;
        public final int selfHp;
        public final int opponentHp;
        public final int selfMaxHp;
        public final int opponentMaxHp;
        public final int selfStance;
        public final int selfMaxStance;
        public final int opponentStance;
        public final int opponentMaxStance;
        public final int roundCount;
        public final boolean guestPresent;
        public final boolean gameOver;
        public final boolean canSubmit;
        public final boolean selfSubmitted;
        public final boolean opponentSubmitted;
        public final String lastMessage;
        public final String roomStage;
        public final String selfImage;
        public final String opponentImage;

        public GameRoomStatus(GameRoom room, PlayerState self, PlayerState opponent) {
            this.roomCode = room.getCode();
            this.selfName = self.getName();
            this.opponentName = opponent != null ? opponent.getName() : "等待對手加入";
            this.selfHp = self.getAvatar().hp;
            this.selfMaxHp = self.getAvatar().maxHp;
            this.selfStance = self.getAvatar().stance;
            this.selfMaxStance = self.getAvatar().maxStance;
            this.opponentHp = opponent != null ? opponent.getAvatar().hp : 0;
            this.opponentMaxHp = opponent != null ? opponent.getAvatar().maxHp : 0;
            this.opponentStance = opponent != null ? opponent.getAvatar().stance : 0;
            this.opponentMaxStance = opponent != null ? opponent.getAvatar().maxStance : 0;
            this.roundCount = room.roundCount;
            this.guestPresent = room.isGuestPresent();
            this.gameOver = room.gameOver;
            this.selfSubmitted = self.hasSubmitted();
            this.opponentSubmitted = opponent != null && opponent.hasSubmitted();
            this.canSubmit = room.isGuestPresent() && !room.gameOver && !self.hasSubmitted();
            this.lastMessage = room.lastMessage;
            this.roomStage = room.gameOver ? "game_over" : room.isGuestPresent() ? "playing" : "waiting";
            this.selfImage = getImageFromState(self.getAvatar().state);
            this.opponentImage = opponent != null ? getImageFromState(opponent.getAvatar().state) : "idle";
        }

        private static String getImageFromState(String state) {
            if (state == null || state.isEmpty()) {
                return "idle";
            }
            switch (state) {
                case "idle":
                    return "idle";
                case "guard":
                    return "guard";
                case "H_guard":
                    return "H_guard";
                case "get_hit":
                    return "get_hit";
                case "parry":
                    return "parry";
                case "guard_break":
                    return "guard_break";
                case "L_atk":
                    return "L_atk";
                case "M_atk":
                    return "M_atk";
                case "H_atk":
                    return "H_atk";
                case "H_ready":
                    return "H_ready";
                case "win":
                    return "win";
                case "lose":
                    return "lose";
                default:
                    return "idle";
            }
        }
    }
}

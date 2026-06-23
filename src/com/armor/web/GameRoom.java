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
    private boolean opponentLeft = false;
    private Action lastHostAction;
    private Action lastGuestAction;

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
        this.lastHostAction = null;
        this.lastGuestAction = null;
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

    public boolean isOpponentLeft() {
        return opponentLeft;
    }

    public synchronized void leaveRoom(String sessionId) {
        PlayerState player = findPlayer(sessionId);
        if (player != null) {
            opponentLeft = true;
        }
    }

    public synchronized boolean requestRematch(String sessionId) {
        if (!gameOver) return false;
        
        PlayerState player = findPlayer(sessionId);
        if (player == null) return false;
        
        player.setRematchRequested(true);
        
        // 電腦自動同意再戰
        if (computerRoom && guest != null && guest.isComputer()) {
            guest.setRematchRequested(true);
        }
        
        if (host.isRematchRequested() && guest != null && guest.isRematchRequested()) {
            restartGame();
        }
        return true;
    }

    private void restartGame() {
        this.roundCount = 1;
        this.gameOver = false;
        this.lastMessage = "再戰開始！請選擇你的動作。";
        this.lastHostAction = null;
        this.lastGuestAction = null;
        
        host.getAvatar().hp = host.getAvatar().maxHp;
        host.getAvatar().stance = host.getAvatar().maxStance;
        host.getAvatar().state = "idle";
        host.getAvatar().isStunned = false;
        host.getAvatar().stunReason = "";
        host.getAvatar().vuln = 0;
        host.getAvatar().nextVuln = 0;
        host.resetAction();
        host.setRematchRequested(false);
        host.getAvatar().resetMultipliers();
        
        if (guest != null) {
            guest.getAvatar().hp = guest.getAvatar().maxHp;
            guest.getAvatar().stance = guest.getAvatar().maxStance;
            guest.getAvatar().state = "idle";
            guest.getAvatar().isStunned = false;
            guest.getAvatar().stunReason = "";
            guest.getAvatar().vuln = 0;
            guest.getAvatar().nextVuln = 0;
            guest.resetAction();
            guest.setRematchRequested(false);
            guest.getAvatar().resetMultipliers();
        }
    }

    public synchronized boolean addGuest(String guestName, String sessionId) {
        if (this.guest != null) {
            return false;
        }
        this.guest = new PlayerState(guestName, sessionId);
        this.lastMessage = "對手已加入，開始對戰！";
        return true;
    }

    public synchronized boolean submitAction(String sessionId, Action action) {
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

    public synchronized GameRoomStatus getStatusForSession(String sessionId) {
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
        Action hostAction = host.getAvatar().isStunned ? Action.NONE : host.getAction();
        Action guestAction = guest.getAvatar().isStunned ? Action.NONE : guest.getAction();
        
        this.lastHostAction = hostAction;
        this.lastGuestAction = guestAction;
        
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
            } else if (guest.getAvatar().hp > host.getAvatar().hp) {
                lastMessage = guest.getName() + " 勝利！ " + roundSummary;
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
        public final int opponentStance;
        public final int selfMaxStance;
        public final int opponentMaxStance;
        public final int roundCount;
        public final boolean guestPresent;
        public final boolean gameOver;
        public final boolean canSubmit;
        public final boolean selfSubmitted;
        public final boolean opponentSubmitted;
        public final String selfState;
        public final String opponentState;
        public final boolean selfStunned;
        public final boolean opponentStunned;
        public final String selfStunReason;
        public final String opponentStunReason;
        public final boolean selfRematchRequested;
        public final boolean opponentRematchRequested;
        public final boolean opponentLeft;
        public final String selfAction;
        public final String opponentAction;
        public final String lastMessage;
        public final String roomStage;

        public GameRoomStatus(GameRoom room, PlayerState self, PlayerState opponent) {
            this.roomCode = room.getCode();
            this.selfName = self.getName();
            this.opponentName = opponent != null ? opponent.getName() : "等待對手加入";
            this.selfHp = self.getAvatar().hp;
            this.opponentHp = opponent != null ? opponent.getAvatar().hp : 0;
            this.selfMaxHp = self.getAvatar().maxHp;
            this.opponentMaxHp = opponent != null ? opponent.getAvatar().maxHp : 100;
            this.selfStance = self.getAvatar().stance;
            this.opponentStance = opponent != null ? opponent.getAvatar().stance : 0;
            this.selfMaxStance = self.getAvatar().maxStance;
            this.opponentMaxStance = opponent != null ? opponent.getAvatar().maxStance : 15;
            this.selfState = self.getAvatar().state;
            this.opponentState = opponent != null ? opponent.getAvatar().state : "idle";
            this.selfStunned = self.getAvatar().isStunned;
            this.opponentStunned = opponent != null ? opponent.getAvatar().isStunned : false;
            this.selfStunReason = self.getAvatar().stunReason;
            this.opponentStunReason = opponent != null ? opponent.getAvatar().stunReason : "";
            this.selfRematchRequested = self.isRematchRequested();
            this.opponentRematchRequested = opponent != null ? opponent.isRematchRequested() : false;
            this.opponentLeft = room.isOpponentLeft();
            this.roundCount = room.roundCount;
            this.guestPresent = room.isGuestPresent();
            this.gameOver = room.gameOver;
            this.selfSubmitted = self.hasSubmitted();
            this.opponentSubmitted = opponent != null && opponent.hasSubmitted();
            this.canSubmit = room.isGuestPresent() && !room.gameOver && !self.hasSubmitted();
            this.lastMessage = room.lastMessage;
            this.roomStage = room.gameOver ? "game_over" : room.isGuestPresent() ? "playing" : "waiting";
            
            if (self == room.host) {
                this.selfAction = room.lastHostAction != null ? room.lastHostAction.name() : "NONE";
                this.opponentAction = room.lastGuestAction != null ? room.lastGuestAction.name() : "NONE";
            } else {
                this.selfAction = room.lastGuestAction != null ? room.lastGuestAction.name() : "NONE";
                this.opponentAction = room.lastHostAction != null ? room.lastHostAction.name() : "NONE";
            }
        }
    }
}

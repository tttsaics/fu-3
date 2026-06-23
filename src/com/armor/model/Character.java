package com.armor.model;

public class Character {

    // 基礎數值
    public int hp;
    public int maxHp;
    public int stance;
    public int maxStance;

    // 戰鬥狀態
    public String state = "idle"; // 例如: "idle", "guard", "get_hit"
    public boolean isStunned = false; // 是否暈眩
    public String stunReason = ""; // 暈眩原因，例如 "被格檔"、"格檔揮空"

    // 破綻機制
    public int vuln = 0; // 當前破綻
    public int nextVuln = 0; // 下回合破綻

    // 傷害倍率
    public double dmgMult = 1.0; // 造成傷害倍率 (我打人痛不痛)
    public double takenMult = 1.0; // 受傷倍率 (我被打痛不痛)

    // 建構子 (初始化數值)
    public Character(int maxHp, int maxStance) {
        this.maxHp = maxHp;
        this.hp = maxHp; // 一開始血是滿的
        this.maxStance = maxStance;
        this.stance = maxStance; // 一開始架式是滿的
    }

    // 輔助功能：重置回合倍率
    // 每一回合結束時，如果沒暈眩，就回復正常倍率
    public void resetMultipliers() {
        this.dmgMult = 1.0;
        this.takenMult = 1.0;
    }

    // 輔助功能：更新破綻
    // 回合結束時，把下回合的破綻生效，並重置累積值
    public void updateVuln() {
        this.vuln = this.nextVuln;
        this.nextVuln = 0;
    }
}

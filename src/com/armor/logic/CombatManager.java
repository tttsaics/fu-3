package com.armor.logic;

import com.armor.enums.Action;
import com.armor.model.Character;
import com.armor.audio.AudioManager;

public class CombatManager {

    private Character player;
    private Character enemy;
    private AudioManager audio;

    public CombatManager(Character player, Character enemy, AudioManager audio) {
        this.player = player;
        this.enemy = enemy;
        this.audio = audio;
    }

    // 敵人出招，能力有限，只能純隨機
    public Action enemyAI() {
        // 隨機出招
        // 玩家暈眩時用強攻擊補傷害
        if (player.isStunned) {
            return Action.H_ATK;
        } else {
            Action[] possible = {
                    Action.L_ATK, Action.M_ATK, Action.H_ATK, Action.GUARD, Action.PARRY_L, Action.PARRY_H,
                    Action.PARRY_M
            };
            int rnd = (int) (Math.random() * possible.length);
            return possible[rnd];
        }
    }

    // 僅處理數值計算，不處理畫面結果
    public void resolveTurn(Action playerAction, Action enemyAction) {
        // 先重置圖片狀態
        player.state = "idle";
        enemy.state = "idle";

        // 重製暈眩狀態
        player.isStunned = false;
        enemy.isStunned = false;

        // 未防禦回復1點架式
        if (playerAction != Action.GUARD)
            player.stance = Math.min(player.maxStance, player.stance + 1);
        if (enemyAction != Action.GUARD)
            enemy.stance = Math.min(enemy.maxStance, enemy.stance + 1);

        boolean playerHit = false;
        boolean enemyHit = false;
        // 以下用窮舉法列出所有情境(能力有限0v0)
        // 雙方都攻擊 (比速度)
        if (isAttack(playerAction) && isAttack(enemyAction)) {
            int pSpeed = getStartup(playerAction) + player.vuln; // 攻擊前搖加上破綻值
            int eSpeed = getStartup(enemyAction) + enemy.vuln;

            if (pSpeed < eSpeed) { // 玩家快
                enemyHit = true;
            } else if (pSpeed > eSpeed) { // 敵人快
                playerHit = true;
            } else { // 平手
                playerHit = true;
                enemyHit = true;
                audio.playSound("double_counter");
            }
        }
        // 玩家攻擊被防
        else if (isAttack(playerAction) && enemyAction == Action.GUARD) {
            // 扣架式
            enemy.stance -= getStanceDmg(playerAction);
            // 設定圖片
            player.state = getActionImage(playerAction);
            enemy.state = "guard";

            // 破防判定
            if (enemy.stance <= 0) {
                enemy.stance = 0;
                enemy.state = "guard_break";
                enemy.isStunned = true; // 下回合暈眩
                enemy.stunReason = "被破防";
                enemy.takenMult = 1.5; // 下回合受傷加倍
            } else {
                // 未破防：計算破綻
                if (playerAction == Action.H_ATK) {
                    player.nextVuln += 1;
                    enemy.nextVuln += 2; // 強攻擊被防，防守者有破綻
                    enemy.state = "H_guard"; // 特殊防禦圖
                } else {
                    player.nextVuln += 3; // 普通攻擊被防
                }
            }
        }
        // 敵人攻擊被防(邏輯同上，反過來)
        else if (isAttack(enemyAction) && playerAction == Action.GUARD) {
            player.stance -= getStanceDmg(enemyAction);
            enemy.state = getActionImage(enemyAction);
            player.state = "guard";

            if (player.stance <= 0) {
                player.stance = 0;
                player.state = "guard_break";
                player.isStunned = true;
                player.stunReason = "被破防";
                player.takenMult = 1.5;
            } else {
                if (enemyAction == Action.H_ATK) {
                    enemy.nextVuln += 1;
                    player.nextVuln += 2;
                    player.state = "H_guard";
                } else {
                    enemy.nextVuln += 3;
                }
            }
        }
        // 一方攻擊，一方無動作
        // 玩家攻擊
        else if (isAttack(playerAction) && !isAttack(enemyAction) && enemyAction != Action.GUARD
                && !isParry(enemyAction)) {
            enemyHit = true; // 敵人直接受傷
        }
        // 敵人攻擊
        else if (isAttack(enemyAction) && !isAttack(playerAction) && playerAction != Action.GUARD
                && !isParry(playerAction)) {
            playerHit = true; // 玩家直接受傷
        }
        // 玩家攻擊敵人格檔
        else if (isAttack(playerAction) && isParry(enemyAction)) {
            boolean correct = false;
            if (playerAction == Action.L_ATK && enemyAction == Action.PARRY_L)
                correct = true;
            if (playerAction == Action.M_ATK && enemyAction == Action.PARRY_M)
                correct = true;
            if (playerAction == Action.H_ATK && enemyAction == Action.PARRY_H)
                correct = true;

            if (correct) {
                // 格檔成功
                enemy.state = "parry";
                player.state = "guard_break";
                player.isStunned = true; // 玩家暈眩
                player.stunReason = "被格檔";
                enemy.dmgMult = 1.5; // 敵人下回合增傷
            } else {
                // 格檔失敗 (猜錯) -> 視為受傷
                enemyHit = true;
                enemy.takenMult = 1.5; // 當下受傷加倍
            }
        }
        // 敵人攻擊玩家格檔(邏輯同上，反過來)
        else if (isAttack(enemyAction) && isParry(playerAction)) {
            boolean correct = false;
            if (enemyAction == Action.L_ATK && playerAction == Action.PARRY_L)
                correct = true;
            if (enemyAction == Action.M_ATK && playerAction == Action.PARRY_M)
                correct = true;
            if (enemyAction == Action.H_ATK && playerAction == Action.PARRY_H)
                correct = true;

            if (correct) {
                player.state = "parry";
                enemy.state = "guard_break";
                enemy.isStunned = true;
                enemy.stunReason = "被格檔";
                player.dmgMult = 1.5;
            } else {
                playerHit = true;
                player.takenMult = 1.5;
            }
        }
        // 一方格檔，一方防禦 (格檔揮空)
        else if (isParry(playerAction) && enemyAction == Action.GUARD) {
            player.state = "parry"; // 揮空圖
            enemy.state = "guard";
            player.isStunned = true; // 懲罰
            player.stunReason = "格檔揮空";
        }
        else if (isParry(enemyAction) && playerAction == Action.GUARD) {
            enemy.state = "parry";
            player.state = "guard";
            enemy.isStunned = true;
            enemy.stunReason = "格檔揮空";
        }
        // 其他格檔揮空狀況
        else {
            if (isParry(playerAction)) {
                player.state = "parry";
                player.isStunned = true;
                player.stunReason = "格檔揮空";
            }
            if (isParry(enemyAction)) {
                enemy.state = "parry";
                enemy.isStunned = true;
                enemy.stunReason = "格檔揮空";
            }
            // 雙方防禦或發呆
            if (playerAction == Action.GUARD)
                player.state = "guard";
            if (enemyAction == Action.GUARD)
                enemy.state = "guard";
        }
        // 結算傷害
        if (playerHit) {
            int dmg = (int) (getDamage(enemyAction) * enemy.dmgMult * player.takenMult);// 敵人動作傷害*敵人增傷倍率*玩家易傷倍率
            player.hp -= dmg;
            if (player.hp < 0)
                player.hp = 0;
            player.state = "get_hit";
        } else if (isAttack(playerAction) && !player.isStunned) {
            // 如果玩家攻擊且沒被打斷
            player.state = getActionImage(playerAction);
            audio.playSound(player.state);
            if (!isAttack(enemyAction)) {
                // 打中非攻擊狀態的敵人 (例如發呆)，增加破綻
                player.nextVuln += (playerAction == Action.H_ATK) ? 1 : 3;
            }
        }

        if (enemyHit) {
            int dmg = (int) (getDamage(playerAction) * player.dmgMult * enemy.takenMult);
            enemy.hp -= dmg;
            if (enemy.hp < 0)
                enemy.hp = 0;
            enemy.state = "get_hit";
        } else if (isAttack(enemyAction) && !enemy.isStunned) {
            enemy.state = getActionImage(enemyAction);
            audio.playSound(enemy.state);
            if (!isAttack(playerAction)) {
                enemy.nextVuln += (enemyAction == Action.H_ATK) ? 1 : 3;
            }
        }
    }

    // 各項行動資料
    // 判斷是否為攻擊
    private boolean isAttack(Action a) {
        return a == Action.L_ATK || a == Action.M_ATK || a == Action.H_ATK;
    }

    // 判斷是否為格檔
    private boolean isParry(Action a) {
        return a == Action.PARRY_L || a == Action.PARRY_M || a == Action.PARRY_H;
    }

    // 取得攻擊傷害
    private int getDamage(Action a) {
        if (a == Action.L_ATK)
            return 4;
        if (a == Action.M_ATK)
            return 8;
        if (a == Action.H_ATK)
            return 12;
        return 0;
    }

    // 取得前搖 (速度)
    private int getStartup(Action a) {
        if (a == Action.L_ATK)
            return 4;
        if (a == Action.M_ATK)
            return 7;
        if (a == Action.H_ATK)
            return 10;
        return 99; // 非攻擊動作速度極慢
    }

    // 取得架式傷害
    private int getStanceDmg(Action a) {
        if (a == Action.L_ATK)
            return 2;
        if (a == Action.M_ATK)
            return 4;
        if (a == Action.H_ATK)
            return 6;
        return 0;
    }

    // 取得對應圖片名稱
    public String getActionImage(Action a) {
        if (a == Action.L_ATK)
            return "L_atk";
        if (a == Action.M_ATK)
            return "M_atk";
        if (a == Action.H_ATK)
            return "H_atk";
        if (a == Action.GUARD)
            return "guard";
        if (isParry(a))
            return "parry"; // 這裡假設格檔只有一張圖，如果分開可以再細分
        return "idle";
    }

}
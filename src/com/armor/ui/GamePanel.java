package com.armor.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import com.armor.main.ProjectArmor; // 匯入主視窗類別
import com.armor.enums.Action;
import com.armor.enums.GameState;
import com.armor.model.Character;
import com.armor.audio.AudioManager;
import com.armor.resource.ImageManager;
import com.armor.logic.CombatManager;

public class GamePanel extends JPanel {
    // 回合狀態
    private GameState currentState = GameState.READY;
    private Action playerAction = null; // 玩家動作
    private Action enemyAction = null; // 敵人動作

    private Timer turnTimer; // 用來倒數的計時器
    private int timeLeft = 3; // 剩餘時間 (秒)
    private int roundCount = 1; // 目前回合數

    private boolean computerMode = false;

    private Character player;
    private Character enemy;

    private ButtonManager btnManager;

    private AudioManager audio;

    private ImageManager imgMgr;

    private CombatManager combat;

    private ProjectArmor mainFrame; // 儲存主視窗的參照

    public GamePanel(ProjectArmor mainFrame, boolean computerMode) {
        this.mainFrame = mainFrame;
        this.computerMode = computerMode;
        setBackground(Color.white);

        setLayout(null);

        // 初始化腳色
        player = new Character(100, 15);
        enemy = new Character(100, 15);

        // 初始化音效
        audio = new AudioManager();

        btnManager = new ButtonManager(this);// 初始化按鈕

        imgMgr = new ImageManager();// 初始化圖片管理員

        player = new Character(100, 15);
        enemy = new Character(100, 15);
        audio = new AudioManager();

        combat = new CombatManager(player, enemy, audio);// 初始化戰鬥運算

        // 監聽視窗大小變化，重新排列按鈕
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                btnManager.repositionButtons();
            }
        });

        turnTimer = new Timer(1000, e -> {
            if (currentState == GameState.INPUT) {
                timeLeft--;
                repaint(); // 重畫畫面更新數字

                if (timeLeft < 0) {
                    // 時間到！強制結束輸入
                    System.out.println("時間到！");
                    if (playerAction == null) {
                        // 如果玩家沒按，當作沒動作
                        System.out.println("玩家未行動");
                        // 這裡之後要接「結算流程」
                    }
                    endInputPhase();
                }
            }
        });

        audio.playBGM("bgm");

        // 遊戲一開始先進入第一回合
        startRound();
    }

    // 回合進行中的函式
    private void startRound() {
        currentState = GameState.INPUT; // 設定狀態為輸入
        timeLeft = 3; // 重置時間
        playerAction = null; // 清空上一回合動作
        enemyAction = null;

        // 啟用按鈕
        btnManager.setButtonsEnabled(true);

        // 啟動倒數
        turnTimer.start();
    }

    // 回合結算函式
    private void endInputPhase() {
        currentState = GameState.PROCESSING;
        turnTimer.stop();
        btnManager.setButtonsEnabled(false); // 鎖住按鈕不能再按

        // 判斷是否暈眩
        if (player.isStunned)
            playerAction = Action.NONE;

        // 敵人 AI(能力有限，我們用的純隨機0vo)決定動作(如果暈眩就不能動)
        if (enemy.isStunned) {
            enemyAction = Action.NONE;
        } else {
            enemyAction = combat.enemyAI();
        }

        System.out.println("結算: 玩家=" + playerAction + " vs 敵人=" + enemyAction);

        // 計算結果
        processTurn();
    }

    // 戰鬥運篹
    private void processTurn() {
        // 1. 數值結算
    combat.resolveTurn(playerAction, enemyAction);

        // 條件 A: 玩家顯示格擋姿勢 且 敵人變成破防姿勢 (代表敵人攻擊被抓到)
        boolean playerParrySuccess = "parry".equals(player.state) && "guard_break".equals(enemy.state);
        
        // 條件 B: 敵人顯示格擋姿勢 且 玩家變成破防姿勢 (代表玩家攻擊被抓到)
        boolean enemyParrySuccess = "parry".equals(enemy.state) && "guard_break".equals(player.state);

        // 只有在上述兩種情況下，才播放特殊演出
        if (playerParrySuccess || enemyParrySuccess) {
            playParrySequence(); 
        } else {
            // 一般情況 (包含雙方空揮格擋、普通攻擊、防禦等)
            // 不需要演戲，直接顯示結果
            repaint();
            startNextRoundTimer();
        }
    }

    private void playParrySequence() {
        // 備份原本計算好的最終狀態 (也就是 parry 和 guard_break 的狀態)
        String finalPlayerState = player.state;
        String finalEnemyState = enemy.state;

        // 第一階段：演出攻擊】
        // 把狀態改回「攻擊當下」的樣子
        if ("parry".equals(player.state)) {
            // 情況：玩家格檔，代表敵人是攻擊方
            enemy.state = combat.getActionImage(enemyAction); // 強制顯示敵人的攻擊圖
            player.state = "idle"; // 玩家先站著 (或顯示防禦圖)
            audio.playSound(enemy.state); // 播放敵人的攻擊音效 (揮刀聲)
        } else {
            // 情況：敵人格檔，代表玩家是攻擊方
            player.state = combat.getActionImage(playerAction); // 強制顯示玩家攻擊圖
            enemy.state = "idle";
            audio.playSound(player.state); // 播放玩家的攻擊音效
        }
        
        // 更新畫面 (顯示攻擊動作)
        repaint(); 

        // 【第二階段：延遲後演出格檔】
        // 設定一個極短的計時器 (例如 150~200 毫秒)
        Timer impactTimer = new Timer(200, e -> {
            // 1. 還原成正確的結果狀態 (Parry圖)
            player.state = finalPlayerState;
            enemy.state = finalEnemyState;

            // 2. 播放格檔音效
            audio.playSound("parry"); 

            // 3. 畫面更新 (顯示格檔與暈眩)
            repaint();

            // 4. 進入下一回合的倒數
            startNextRoundTimer();
        });
        
        impactTimer.setRepeats(false); // 只執行一次
        impactTimer.start();
    }
    private void startNextRoundTimer() {
        Timer stepTimer = new Timer(1000, e -> {
            prepareNextRound();
        });
        stepTimer.setRepeats(false);
        stepTimer.start();
    }

    // 準備下一回合
    private void prepareNextRound() {
        roundCount++;

        // 更新破綻值
        player.updateVuln();
        enemy.updateVuln();
        player.nextVuln = 0; // 歸零
        enemy.nextVuln = 0;

        // 玩家沒暈就重置倍率
        if (!player.isStunned) {
            player.resetMultipliers();
        }
        // 敵人沒暈就重置倍率
        if (!enemy.isStunned) {
            enemy.resetMultipliers();
        }
        // 檢查遊戲結束
        if (player.hp <= 0 || enemy.hp <= 0 || roundCount > 50) {
            currentState = GameState.GAME_OVER;
            audio.stopBGM();
            if (player.hp > enemy.hp) {
                System.out.println("VICTORY");
                player.state = "win";
                enemy.state = "lose";
                audio.playSound("you_win");
                // 使用 invokeLater 確保畫面先重畫(顯示勝利姿勢)後，再彈出視窗
                SwingUtilities.invokeLater(() -> mainFrame.handleGameOver("You Win!"));

            } else {
                System.out.println("DEFEAT");
                player.state = "lose";
                enemy.state = "win";
                audio.playSound("you_lose");
                SwingUtilities.invokeLater(() -> mainFrame.handleGameOver("You Lose!"));
            }
            repaint(); // 重畫一次以顯示結束畫面
        } else {
            // 繼續下一回合
            startRound();
        }
    }

    // 畫血條，回合數的函式
    private void drawUI(Graphics g, int w, int h) {

        Graphics2D g2d = (Graphics2D) g;

        // 計算血條寬度，拉長至螢幕中間，中間留出方框空隙
        int barWidth = w / 2 - 60; 
        int barHeight = 25; // 血條高度
        int stanceHeight = 10; // 架式條高度 (比較細)

        int pX = 20; // 玩家血條位置在左上
        int eX = w / 2 + 40; // 敵人血條位置在右上
        int topY = 20; // 距離頂部

        g2d.setStroke(new BasicStroke(3));

        // 畫玩家血條
        // 背景
        g.setColor(Color.BLACK);
        g.fillRect(pX, topY, barWidth, barHeight);
        // 畫目前血量 (紅色)
        g.setColor(new Color(220, 50, 50)); // 深紅色
        double pHpRatio = (double) player.hp / player.maxHp;// 當前血量與總血量比例
        g.fillRect(pX, topY, (int) (barWidth * pHpRatio), barHeight);// 填充血量
        // 血條邊框
        g.setColor(Color.BLACK);
        g.drawRect(pX, topY, barWidth, barHeight);
        
        // 架式條
        int stanceY = topY + barHeight + 5; // 往下移一點
        g.setColor(Color.BLACK); // 架式背景
        g.fillRect(pX, stanceY, barWidth, stanceHeight);
        g.setColor(Color.GRAY); // 架式本體
        double pStanceRatio = (double) player.stance / player.maxStance;
        g.fillRect(pX, stanceY, (int) (barWidth * pStanceRatio), stanceHeight);
        g.setColor(Color.BLACK);
        g.drawRect(pX, stanceY, barWidth, stanceHeight);

        // 畫敵人血條 (向右對齊並往外扣減，達成由內向外扣血效果)
        g.setColor(Color.BLACK);
        g.fillRect(eX, topY, barWidth, barHeight);
        // 畫目前血量 (紅色)
        g.setColor(new Color(220, 50, 50)); // 深紅色
        double eHpRatio = (double) enemy.hp / enemy.maxHp;// 當前血量與總血量比例
        int currentHpW = (int) (barWidth * eHpRatio);
        g.fillRect(w - 20 - currentHpW, topY, currentHpW, barHeight);// 填充血量
        // 血條邊框
        g.setColor(Color.BLACK);
        g.drawRect(eX, topY, barWidth, barHeight);
        
        // 架式條 (向右對齊)
        g.setColor(Color.BLACK); // 架式背景
        g.fillRect(eX, stanceY, barWidth, stanceHeight);
        g.setColor(Color.GRAY); // 架式本體
        double eStanceRatio = (double) enemy.stance / enemy.maxStance;
        int currentStanceW = (int) (barWidth * eStanceRatio);
        g.fillRect(w - 20 - currentStanceW, stanceY, currentStanceW, stanceHeight);
        g.setColor(Color.BLACK);// 架式邊框
        g.drawRect(eX, stanceY, barWidth, stanceHeight);

        // 畫中央計時器方框
        int timerX = w / 2 - 30;
        int timerY = topY;
        int timerW = 60;
        int timerH = 60;

        g.setColor(Color.BLACK);
        g.fillRect(timerX, timerY, timerW, timerH);
        
        g2d.setStroke(new BasicStroke(3));
        g.setColor(Color.DARK_GRAY);
        g.drawRect(timerX, timerY, timerW, timerH);

        // 畫計時文字
        g.setColor(Color.WHITE);
        g.setFont(new Font("Impact", Font.BOLD, 30));
        String timeStr;
        if (currentState == GameState.INPUT) {
            timeStr = String.valueOf(timeLeft);
        } else if (currentState == GameState.GAME_OVER) {
            timeStr = "GG";
        } else {
            timeStr = "VS";
        }
        FontMetrics fmTimer = g.getFontMetrics();
        int strTimerW = fmTimer.stringWidth(timeStr);
        int strTimerH = fmTimer.getAscent() - fmTimer.getDescent();
        g.drawString(timeStr, timerX + (timerW - strTimerW) / 2, timerY + (timerH + strTimerH) / 2 - 2);

        // 畫回合數 (移到計時器下方)
        g.setColor(Color.BLACK);
        g.setFont(new Font("Impact", Font.BOLD, 28));
        String roundStr = "ROUND " + roundCount;
        int strW = g.getFontMetrics().stringWidth(roundStr);
        g.drawString(roundStr, (w - strW) / 2, timerY + timerH + 30);
    }

    // 畫面繪製
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 清除畫面

        // 取得當前的寬跟高 因為使用者可以改變
        int currentW = getWidth();
        int currentH = getHeight();

        // 設定文字顏色與大小
        g.setColor(Color.black);
        g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 40));

        // 動態排版：基於寬度計算角色大小，避免 16:10 等高螢幕比例下角色過度放大導致下半身被裁切
        int displayH = (int) (currentW * 1.125);

        // 玩家跟敵人位置高度相同
        int charY = 0;// 下半身可以被邊界切到

        // 取的要畫的圖
        Image pImg = imgMgr.getImage(player.state);
        if (pImg == null)
            pImg = imgMgr.getImage("idle");
        Image eImg = imgMgr.getImage(enemy.state);
        if (eImg == null)
            eImg = imgMgr.getImage("idle");

        if (pImg != null) {
            // 圖片原本的寬高
            int originalW = pImg.getWidth(null);
            int originalH = pImg.getHeight(null);
            // 圖片長寬比
            double ratio = (originalH > 0) ? (double) originalW / originalH : 1.0;
            // 計算需顯示的寬度
            int displayW = (int) (displayH * ratio);
            // 畫玩家
            int playerX = -20; // 靠近中間一點
            if (pImg != null) {
                g.drawImage(pImg, playerX, charY, displayW, displayH, null);
            }
        }
        // 畫敵人
        if (eImg != null) {
            int originalW = eImg.getWidth(null);
            int originalH = eImg.getHeight(null);

            double ratio = (originalH > 0) ? (double) originalW / originalH : 1.0;
            // 計算需顯示的寬度
            int displayW = (int) (displayH * ratio);
            int enemyX = (int) (currentW)+20 - displayW; // 靠近中間一點
            g.drawImage(eImg, enemyX + displayW, charY, -displayW, displayH, null);
        }

        // 畫按鈕區
        // 計算區域範圍
        int startY = currentH * 3 / 4;// 起始點
        int areaH = currentH / 4;// 高度

        // 畫按鈕區
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, startY, currentW, areaH);

        g.setColor(new Color(0, 0, 0));
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(5)); // 線條粗細
        g2d.drawLine(0, startY, currentW, startY);
        g2d.setStroke(new BasicStroke(1)); // 還原粗細

        // 血條
        drawUI(g, currentW, currentH);

        if (computerMode) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 24));
            g.drawString("對手：電腦", 30, 70);
        }
    }

    // 當按鈕被按下時，按鈕管理器會呼叫這個函式，以播放圖片
    public void onPlayerInput(Action action) {
        if (currentState == GameState.INPUT) {
            playerAction = action;
            System.out.println("玩家選擇了: " + playerAction);
            endInputPhase();
        }
    }

    // 攻擊準備動作
    public void previewState(String stateName) {
        // 檢查條件：只有在輸入階段、沒受傷、沒暈眩時才能預覽
        if (currentState == GameState.INPUT && !player.state.equals("get_hit") && !player.isStunned) {
            player.state = stateName; // 直接設定成傳進來的動作名稱 (例如 "idle" 或 "H_ready")
            repaint();
        }
    }
}

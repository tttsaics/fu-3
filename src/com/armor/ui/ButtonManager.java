package com.armor.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import com.armor.enums.Action;

public class ButtonManager {

    private GamePanel panel;
    private List<JButton> buttons = new ArrayList<>();// 用來儲存按鈕

    // 定義按鈕名稱
    private String[] btnNames = {
            "輕攻擊", "防禦", "格檔(輕)",
            "中攻擊", "跳過", "格檔(中)",
            "強攻擊", "", "格檔(強)"
    };

    public ButtonManager(GamePanel panel) {
        this.panel = panel;
        initButtons();// 初始化
    }

    // 生成按鈕
    private void initButtons() {
        // 定義動作陣列 對應按鈕0~8
        Action[] actions = {
                Action.L_ATK, Action.GUARD, Action.PARRY_L,
                Action.M_ATK, Action.NONE, Action.PARRY_M,
                Action.H_ATK, Action.NONE, Action.PARRY_H
        };
        // 依照 3x3 的邏輯產生按鈕
        // 0 1 2
        // 3 4 5
        // 6 7 8
        for (int i = 0; i < 9; i++) {
            String label = btnNames[i];
            // 如果名稱是空的，就不建立按鈕
            if (label.isEmpty()) {
                buttons.add(null); // 佔位符
                continue;
            }
            JButton btn = new JButton(label);
            btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 18));
            btn.setFocusable(false);

            // 優化按鈕外觀
            styleButton(btn);

            // 加入到面板
            panel.add(btn);
            buttons.add(btn);

            final int index = i;

            btn.addActionListener(e -> {
                // 只有在輸入階段才能按
                panel.onPlayerInput(actions[index]);
            });

            // 定義該按鈕滑鼠移入時，要顯示什麼動作
            String hoverState = null;
            
            if (i == 0) hoverState = "idle";    // 輕攻擊 -> 顯示閒置
            if (i == 3) hoverState = "idle";    // 中攻擊 -> 顯示閒置
            if (i == 6) hoverState = "H_ready"; // 強攻擊 -> 顯示蓄力動作

            // 如果這個按鈕有定義懸停動作，就加入監聽器
            if (hoverState != null) {
                final String stateToShow = hoverState; // 需要用 final 變數傳入匿名類別
                
                btn.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        // 呼叫我們剛剛在 GamePanel 改寫的新函式
                        panel.previewState(stateToShow); 
                    }

                    public void mouseExited(MouseEvent e) {
                        // 滑鼠移出時，統一變回 idle
                        panel.previewState("idle"); 
                    }
                });
            }
        }
    }

    public void setButtonsEnabled(boolean enabled) {
        for (JButton btn : buttons) {
            if (btn != null) {
                if ("跳過".equals(btn.getText())) {
                    btn.setEnabled(true);
                } else {
                    btn.setEnabled(enabled);
                }
            }
        }
    }

    // 重新計算按鈕位置
    public void repositionButtons() {
        int w = panel.getWidth();
        int h = panel.getHeight();

        // 按鈕區塊佔下方 1/4
        int startY = h * 3 / 4;
        int areaH = h / 4;

        // 按鈕大小與間距計算
        int margin = 10;
        int btnW = (w / 3) - (margin * 2); // 三欄
        int btnH = (areaH / 3) - (margin * 2); // 三列

        int guardBtnH = areaH - (margin * 2);// 為防禦另外製作按鈕

        // 限制按鈕最大高度，避免視窗拉長時按鈕變得超級巨大
        if (btnH > 60)
            btnH = 60;

        // 迴圈設定每個按鈕的位置
        for (int i = 0; i < buttons.size(); i++) {
            JButton btn = buttons.get(i);
            if (btn == null)
                continue;

            int row = i / 3; // 第幾列
            int col = i % 3; // 第幾欄

            // 計算座標
            int x = (w / 3) * col + margin;
            if (i == 1) {
                int y = startY + margin;
                btn.setBounds(x, y, btnW, guardBtnH);
            } else {
                int y = startY + (areaH / 3) * row + margin;

                btn.setBounds(x, y, btnW, btnH);
            }
        }
    }

    // 按鈕樣式改變
    private void styleButton(JButton btn) {
        // 顏色
        Color normalColor = new Color(200, 200, 200);
        Color hoverColor = new Color(255, 255, 255);
        Color textColor = Color.BLACK;

        btn.setBackground(normalColor);
        btn.setForeground(textColor);
        btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20)); // 字體加大
        btn.setFocusPainted(false); // 去除點擊後的虛線框 (很醜)
        btn.setBorder(BorderFactory.createLineBorder(Color.black, 2));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor); // 移入變亮
                btn.setOpaque(true);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalColor); // 移出變回深色
            }
        });
    }
}

package com.armor.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.armor.main.ProjectArmor; // 匯入主程式，因為要呼叫它切換畫面

public class MenuPanel extends JPanel {

    private ProjectArmor mainFrame; // 紀錄主視窗是誰
    private JButton btnStart, btnComputer, btnTutorial, btnExit;

    public MenuPanel(ProjectArmor mainFrame) {
        this.mainFrame = mainFrame;
        
        setBackground(Color.WHITE); // 還原為白色主題
        setLayout(null); // 自由排版

        initButtons();
    }

    private void initButtons() {
        // 設定字體
        Font btnFont = new Font("Microsoft JhengHei", Font.BOLD, 30);
        
        // 1. 開始遊戲按鈕
        btnStart = createButton("開始遊戲", btnFont);
        btnStart.addActionListener(e -> {
            // 呼叫主程式的 startGame 方法
            mainFrame.startGame();
        });
        add(btnStart);

        // 2. 電腦對戰按鈕
        btnComputer = createButton("電腦對戰", btnFont);
        btnComputer.addActionListener(e -> {
            mainFrame.startGame(true);
        });
        add(btnComputer);

        // 3. 新手教學按鈕
        btnTutorial = createButton("新手教學", btnFont);
        btnTutorial.addActionListener(e -> {
            // 呼叫主程式的 showTutorial 方法 (使用新風格視窗)
            mainFrame.showTutorial();
        });
        add(btnTutorial);

        // 4. 結束遊戲按鈕
        btnExit = createButton("結束遊戲", btnFont);
        btnExit.addActionListener(e -> {
            System.exit(0); // 直接關閉程式
        });
        add(btnExit);
    }

    // 輔助函式：建立統一樣式的按鈕
    private JButton createButton(String text, Font font) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));
        
        // 滑鼠移入變色效果
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(Color.YELLOW);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
            }
        });
        return btn;
    }

    // 重新計算按鈕位置 (置中)
    public void repositionComponents() {
        int w = getWidth();
        int h = getHeight();
        
        int btnW = 300;
        int btnH = 60;
        int gap = 20; // 按鈕間距
        
        int totalHeight = btnH * 4 + gap * 3;
        int startY = Math.max((h - totalHeight) / 2, 50);

        btnStart.setBounds((w - btnW) / 2, startY, btnW, btnH);
        btnComputer.setBounds((w - btnW) / 2, startY + (btnH + gap) * 1, btnW, btnH);
        btnTutorial.setBounds((w - btnW) / 2, startY + (btnH + gap) * 2, btnW, btnH);
        btnExit.setBounds((w - btnW) / 2, startY + (btnH + gap) * 3, btnW, btnH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 畫超大標題 (改為深色以在白底可見)
        g.setColor(Color.BLACK);
        // 設定字體：字體名稱, 粗體, 大小(120)
        g.setFont(new Font("Impact", Font.BOLD, 120)); 
        
        String title = "ARMOR";
        FontMetrics fm = g.getFontMetrics();
        int titleW = fm.stringWidth(title);
        
        // 水平置中，垂直位置在畫面 1/3 處
        g.drawString(title, (getWidth() - titleW) / 2, getHeight() / 3);
        
        // 在標題下面加個副標題 (深灰色)
        g.setFont(new Font("Arial", Font.ITALIC, 30));
        g.setColor(Color.DARK_GRAY);
        String sub = "The Ultimate Duel";
        int subW = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (getWidth() - subW) / 2, getHeight() / 3 + 50);
        
        // 確保按鈕位置正確
        repositionComponents();
    }
}
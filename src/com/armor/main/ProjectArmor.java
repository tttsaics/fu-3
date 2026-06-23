package com.armor.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.armor.ui.GamePanel; // 匯入遊戲面板
import com.armor.ui.MenuPanel; //匯入首頁面板

public class ProjectArmor extends JFrame {

    public static final int INIT_WIDTH = 1024;// 預設視窗大小
    public static final int INIT_HEIGHT = 768;
    private boolean computerMode = false;

    public ProjectArmor() {

        setTitle("ARMOR");
        setSize(INIT_WIDTH, INIT_HEIGHT);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// 按叉關閉

        setResizable(true);// 可調整視窗大小

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        showMenu();//先顯示首頁

        setVisible(true);// 顯示視窗
    }

    public void showMenu() {
        // 清除目前的內容 
        getContentPane().removeAll();

        // 使用 BorderLayout 把 MenuPanel 放到中央，確保會自動填滿視窗
        getContentPane().setLayout(new BorderLayout());

        // 建立首頁面板，並把 "this" (主視窗) 傳進去
        MenuPanel menu = new MenuPanel(this);
        getContentPane().add(menu, BorderLayout.CENTER);
        menu.requestFocusInWindow();

        // 通知 Swing 畫面改變了，要重新整理
        revalidate();
        repaint();
    }

    public void startGame() {
        startGame(false);
    }

    public void startGame(boolean computerMode) {
        this.computerMode = computerMode;

        // 清除首頁
        getContentPane().removeAll();
        
        // 加入遊戲面板
        GamePanel game = new GamePanel(this, computerMode); // 將主視窗傳入 GamePanel
        add(game);
        
        // 讓遊戲面板取得焦點 (這樣鍵盤操作才會有反應)
        game.requestFocusInWindow();
        
        // 重新整理畫面
        revalidate();
        repaint();
    }

    /**
     * 顯示新手教學視窗 (風格與遊戲結束視窗一致)
     */
    public void showTutorial() {
        JDialog dialog = new JDialog(this, "新手教學", true);
        dialog.setUndecorated(true); // 去除視窗邊框，保持風格一致

        // 主要面板 (黑色背景)
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3)); // 白色邊框

        // 標題 (使用 Impact 字體)
        JLabel lblTitle = new JLabel("TUTORIAL");
        lblTitle.setFont(new Font("Impact", Font.BOLD, 50));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 說明內容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Color.BLACK);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        String[] instructions = {
            "1. 雙方同時出招，比速度 (輕 > 中 > 強)",
            "2. 攻擊被防禦會累積架式值，滿了會被破防",
            "3. 猜中攻擊並格擋 (Parry) 可造成暈眩",
            "4. 暈眩時受傷會加倍！"
        };

        for (String text : instructions) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
            lbl.setForeground(Color.LIGHT_GRAY);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(lbl);
            contentPanel.add(Box.createVerticalStrut(10)); // 文字間距
        }

        // 按鈕面板
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.BLACK);
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

        JButton btnOk = createStyledButton("了解");
        btnOk.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnOk);

        // 加入元件並調整間距
        panel.add(Box.createVerticalStrut(30));
        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(contentPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnPanel);
        panel.add(Box.createVerticalStrut(20));

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this); // 顯示在視窗中間
        dialog.setVisible(true);
    }

    /**
     * 處理遊戲結束：當血量歸零時彈出視窗
     * @param result 傳入 "You Win" 或 "You Lose"
     */
    public void handleGameOver(String result) {
        // 建立自訂對話框以符合首頁風格
        JDialog dialog = new JDialog(this, "遊戲結束", true); // true 代表 modal (鎖定主視窗)
        dialog.setUndecorated(true); // 去除視窗邊框，讓風格更像遊戲原生介面
        
        // 主要面板 (黑色背景)
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3)); // 白色邊框

        // 顯示結果文字 (使用 Impact 字體，類似首頁標題)
        JLabel lblResult = new JLabel(result);
        lblResult.setFont(new Font("Impact", Font.BOLD, 50)); 
        lblResult.setForeground(Color.WHITE);
        lblResult.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 提示文字
        JLabel lblMsg = new JLabel("PLAY AGAIN?");
        lblMsg.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        lblMsg.setForeground(Color.LIGHT_GRAY);
        lblMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 按鈕面板
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.BLACK);
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

        JButton btnRestart = createStyledButton("重新遊戲");
        btnRestart.addActionListener(e -> {
            dialog.dispose(); // 關閉對話框
            startGame();
        });

        JButton btnMenu = createStyledButton("回首頁");
        btnMenu.addActionListener(e -> {
            dialog.dispose(); // 關閉對話框
            showMenu();
        });

        btnPanel.add(btnRestart);
        btnPanel.add(btnMenu);

        // 加入元件並調整間距
        panel.add(Box.createVerticalStrut(30));
        panel.add(lblResult);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblMsg);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnPanel);
        panel.add(Box.createVerticalStrut(20));

        dialog.add(panel);
        dialog.pack(); // 自動調整大小
        dialog.setLocationRelativeTo(this); // 顯示在視窗中間
        dialog.setVisible(true);
    }

    // 輔助方法：建立風格化按鈕 (與 MenuPanel 風格一致)
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.BLACK);
        // 複合邊框：外層灰線，內層留白
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 3),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

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

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new ProjectArmor();
        });
    }
}
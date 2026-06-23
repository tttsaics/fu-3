package com.armor.enums; 

public enum GameState {
    READY,      // 回合開始前
    INPUT,      // 輸入階段
    PROCESSING, // 結算中
    GAME_OVER   // 遊戲結束
}
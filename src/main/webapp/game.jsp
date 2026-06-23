<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String roomCode = (String) session.getAttribute("roomCode");
    Integer playerIndex = (Integer) session.getAttribute("playerIndex");
    if (roomCode == null || playerIndex == null) {
        response.sendRedirect("index.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="zh-Hant">
<head>
    <meta charset="UTF-8">
    <title>ARMOR - 對戰房間</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="page-container">
    <header>
        <h1>ARMOR</h1>
        <p>房間代碼：<strong><%= roomCode %></strong></p>
    </header>

    <div class="content-box">
        <section class="box game-container">
            <div class="battle-area">
                <div class="character-section">
                    <h3>你</h3>
                    <div class="character-display">
                        <img id="selfImage" src="image/idle.png" alt="你的角色" class="character-sprite">
                    </div>
                    <p id="selfName">-</p>
                    <div class="hp-bar">
                        <div id="selfHpBar" class="hp-fill" style="width: 100%"></div>
                    </div>
                    <p>血量：<span id="selfHp">-</span>/<span id="selfMaxHp">-</span></p>
                </div>

                <div class="vs-text">VS</div>

                <div class="character-section">
                    <h3>對手</h3>
                    <div class="character-display">
                        <img id="opponentImage" src="image/idle.png" alt="對手角色" class="character-sprite">
                    </div>
                    <p id="opponentName">-</p>
                    <div class="hp-bar">
                        <div id="opponentHpBar" class="hp-fill" style="width: 100%"></div>
                    </div>
                    <p>血量：<span id="opponentHp">-</span>/<span id="opponentMaxHp">-</span></p>
                </div>
            </div>
        </section>

        <section class="box status-box">
            <div class="board-row">
                <div>
                    <h3>狀態</h3>
                    <p id="roomStatus">等待狀態...</p>
                </div>
                <div>
                    <h3>回合</h3>
                    <p id="roundCount">-</p>
                </div>
            </div>
            <p id="lastMessage">請稍候，系統會自動更新。</p>
        </section>

        <section class="box action-box">
            <h2>選擇動作</h2>
            <div class="buttons-grid">
                <button type="button" onclick="submitAction('L_ATK')">輕攻擊</button>
                <button type="button" onclick="submitAction('GUARD')">防禦</button>
                <button type="button" onclick="submitAction('PARRY_L')">格檔(輕)</button>
                <button type="button" onclick="submitAction('M_ATK')">中攻擊</button>
                <button type="button" class="empty">&nbsp;</button>
                <button type="button" onclick="submitAction('PARRY_M')">格檔(中)</button>
                <button type="button" onclick="submitAction('H_ATK')">強攻擊</button>
                <button type="button" class="empty">&nbsp;</button>
                <button type="button" onclick="submitAction('PARRY_H')">格檔(強)</button>
            </div>
            <p id="submitHint">按鈕將於對手加入後啟用。</p>
        </section>

        <section class="box">
            <button class="button" onclick="window.location.href='index.jsp'">離開房間</button>
        </section>
    </div>
</div>

<script>
    const refreshInterval = 1200;
    const playerIndex = <%= playerIndex %>;
    const roomCode = "<%= roomCode %>";

    async function fetchState() {
        try {
            const response = await fetch(`game?action=state`);
            if (!response.ok) {
                throw new Error('無法取得遊戲狀態');
            }
            const data = await response.json();
            if (!data.success) {
                throw new Error(data.message || '伺服器回應錯誤');
            }
            updatePage(data);
        } catch (error) {
            console.error(error);
            document.getElementById('roomStatus').textContent = '連線異常，請重新整理';
        }
    }

    async function submitAction(action) {
        try {
            const body = new URLSearchParams();
            body.append('action', 'submit');
            body.append('playerAction', action);

            const response = await fetch('game', {
                method: 'POST',
                body: body,
            });
            const data = await response.json();
            if (!data.success) {
                document.getElementById('submitHint').textContent = data.message;
                return;
            }
            updatePage(data);
        } catch (error) {
            console.error(error);
            document.getElementById('submitHint').textContent = '送出動作失敗，請稍候重試。';
        }
    }

    function updatePage(data) {
        document.getElementById('selfName').textContent = data.selfName;
        document.getElementById('opponentName').textContent = data.opponentName;
        document.getElementById('selfHp').textContent = data.selfHp;
        document.getElementById('selfMaxHp').textContent = data.selfMaxHp;
        document.getElementById('opponentHp').textContent = data.opponentHp;
        document.getElementById('opponentMaxHp').textContent = data.opponentMaxHp;
        document.getElementById('roundCount').textContent = data.roundCount;
        document.getElementById('lastMessage').textContent = data.lastMessage;

        // 更新角色圖片
        if (data.selfImage) {
            document.getElementById('selfImage').src = 'image/' + data.selfImage + '.png';
        }
        if (data.opponentImage) {
            document.getElementById('opponentImage').src = 'image/' + data.opponentImage + '.png';
        }

        // 更新血量條
        const selfHpPercent = (data.selfHp / data.selfMaxHp) * 100;
        const opponentHpPercent = (data.opponentHp / data.opponentMaxHp) * 100;
        document.getElementById('selfHpBar').style.width = Math.max(0, selfHpPercent) + '%';
        document.getElementById('opponentHpBar').style.width = Math.max(0, opponentHpPercent) + '%';

        if (data.roomStage === 'waiting') {
            document.getElementById('roomStatus').textContent = '等待對手加入...';
            disableButtons(true);
            document.getElementById('submitHint').textContent = '請等待對手加入並開始遊戲。';
        } else if (data.gameOver) {
            document.getElementById('roomStatus').textContent = '遊戲結束';
            disableButtons(true);
            document.getElementById('submitHint').textContent = '遊戲結束，請返回首頁。';
        } else {
            document.getElementById('roomStatus').textContent = data.selfSubmitted ? '已選擇，等待對手' : '請選擇你的動作';
            disableButtons(!data.canSubmit);
            document.getElementById('submitHint').textContent = data.canSubmit ? '請選擇一個動作並送出。' : '已送出動作，等待對手。';
        }
    }

    function disableButtons(disabled) {
        document.querySelectorAll('.buttons-grid button:not(.empty)').forEach(btn => {
            btn.disabled = disabled;
        });
    }

    fetchState();
    setInterval(fetchState, refreshInterval);
</script>
</body>
</html>

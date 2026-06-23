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
    <link rel="stylesheet" href="css/style.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="battle-scene">
    <!-- P1 (Self) Status Overlay -->
    <div class="battle-ui self-ui">
        <div class="ui-name" id="selfBattleName">-</div>
        <div class="battle-hp-bar"><div id="selfHpBar" class="hp-fill"></div></div>
        <div class="battle-stance-bar"><div id="selfStanceBar" class="stance-fill"></div></div>
        <div class="action-detail">
            <div class="counter-text" id="selfCounterText"></div>
            <div class="action-name" id="selfActionName"></div>
        </div>
    </div>
    <!-- P2 (Opponent) Status Overlay -->
    <div class="battle-ui opponent-ui">
        <div class="ui-name" id="opponentBattleName">-</div>
        <div class="battle-hp-bar"><div id="opponentHpBar" class="hp-fill"></div></div>
        <div class="battle-stance-bar"><div id="opponentStanceBar" class="stance-fill"></div></div>
        <div class="action-detail">
            <div class="counter-text" id="opponentCounterText"></div>
            <div class="action-name" id="opponentActionName"></div>
        </div>
    </div>

    <!-- Center Timer Box -->
    <div class="battle-timer-box" id="battleTimerBox">VS</div>

    <!-- Parry Watermark Overlay (z-indexed behind characters) -->
    <div class="parry-watermark" id="parryWatermark">PARRY</div>

    <!-- Header Overlay (Room Code, Title) -->
    <div class="battle-header-overlay">
        <span class="logo">ARMOR</span>
        <span class="room-info">房間代碼：<strong><%= roomCode %></strong></span>
        <span class="round-info">ROUND <span id="roundCount">-</span></span>
    </div>

    <!-- Status Message Overlay -->
    <div class="battle-status-overlay">
        <div id="roomStatus" class="status-title">請選擇你的動作</div>
        <div id="lastMessage" class="status-msg">請稍候，系統會自動更新。</div>
    </div>

    <!-- Leave Button Overlay -->
    <button class="leave-btn" onclick="leaveRoom()">離開房間</button>

    <!-- Characters -->
    <div class="character-container">
        <div class="character self">
            <img id="selfImg" src="image/idle.png" alt="Self">
        </div>
        <div class="character opponent">
            <img id="opponentImg" src="image/idle.png" alt="Opponent">
        </div>
    </div>

    <!-- Bottom Controls Overlay -->
    <div class="battle-controls-overlay">
        <div class="buttons-grid">
            <button type="button" onclick="submitAction('L_ATK')">輕攻擊</button>
            <button type="button" onclick="submitAction('GUARD')">防禦</button>
            <button type="button" onclick="submitAction('PARRY_L')">格檔(輕)</button>
            <button type="button" onclick="submitAction('M_ATK')">中攻擊</button>
            <button type="button" id="skipButton" class="skip-btn" onclick="submitAction('NONE')">跳過</button>
            <button type="button" onclick="submitAction('PARRY_M')">格檔(中)</button>
            <button type="button" onclick="submitAction('H_ATK')">強攻擊</button>
            <button type="button" class="empty">&nbsp;</button>
            <button type="button" onclick="submitAction('PARRY_H')">格檔(強)</button>
        </div>
        <p id="submitHint">按鈕將於對手加入後啟用。</p>
    </div>
</div>

<!-- Modal 覆蓋層 -->
<div class="modal-overlay" id="gameModal">
    <div class="modal-content">
        <h2 id="modalTitle">標題</h2>
        <p id="modalDesc">內容說明</p>
        <div class="modal-buttons" id="modalButtons">
            <!-- 動態生成按鈕 -->
        </div>
    </div>
</div>

<script>
    const refreshInterval = 1200;
    const playerIndex = <%= playerIndex %>;
    const roomCode = "<%= roomCode %>";
    let lastRoundCount = 0;
    let bgmStarted = false;
    let gameOverHandled = false;
    let resultShown = false;
    let turnTimer = null;
    let turnSeconds = 5;
    
    // HP tracking globals for damage feedback
    let lastSelfHp = 100;
    let lastOpponentHp = 100;
    let isFirstPoll = true;

    function isAttack(action) {
        return action === 'L_ATK' || action === 'M_ATK' || action === 'H_ATK';
    }

    function getActionChineseName(action) {
        if (!action || action === 'NONE') return '無動作';
        switch (action) {
            case 'L_ATK': return '輕攻擊';
            case 'M_ATK': return '中攻擊';
            case 'H_ATK': return '強攻擊';
            case 'GUARD': return '防禦';
            case 'PARRY_L': return '格檔(輕)';
            case 'PARRY_M': return '格檔(中)';
            case 'PARRY_H': return '格檔(強)';
            default: return action;
        }
    }
    const bgm = new Audio('sfx/bgm.wav');
    bgm.loop = true;

    function startBGM() {
        if (!bgmStarted) {
            bgm.play().catch(e => console.log('BGM 播放失敗 (需使用者互動或檔案缺失)'));
            bgmStarted = true;
        }
    }

    function playSound(name, delay = 0) {
        if (!name || name === 'idle') return;
        setTimeout(() => {
            const audio = new Audio('sfx/' + name + '.wav');
            audio.play().catch(e => console.log('音效播放失敗:', name));
        }, delay);
    }
    
    function showModal(title, desc, buttonsHtml) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalDesc').textContent = desc;
        document.getElementById('modalButtons').innerHTML = buttonsHtml;
        document.getElementById('gameModal').style.display = 'flex';
    }

    function hideModal() {
        document.getElementById('gameModal').style.display = 'none';
    }

    async function requestRematch() {
        hideModal();
        try {
            await fetch('game?action=rematch', { method: 'POST' });
            // 在使用者點擊再戰按鈕時重啟 BGM，避開瀏覽器的 Autoplay 限制
            startBGM();
        } catch (e) {
            console.error(e);
        }
    }

    async function leaveRoom() {
        try {
            await fetch('game?action=leave', { method: 'POST' });
        } catch (e) {
            console.error(e);
        }
        window.location.href = 'index.jsp';
    }

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
        startBGM();
        if (turnTimer) {
            clearInterval(turnTimer);
            turnTimer = null;
        }
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

    function actionToState(action) {
        if (action === 'L_ATK') return 'L_atk';
        if (action === 'M_ATK') return 'M_atk';
        if (action === 'H_ATK') return 'H_atk';
        if (action === 'GUARD') return 'guard';
        if (action && action.startsWith('PARRY')) return 'parry';
        return 'idle';
    }

    function updateCharacterImg(id, state, action, isNewRound, isParried, isParrying, opponentState, opponentAction) {
        const img = document.getElementById(id);
        const newState = state || 'idle';
        const isHit = newState === 'get_hit';
        const isAttacking = (newState === 'L_atk' || newState === 'M_atk' || newState === 'H_atk') && opponentState === 'get_hit';
        
        if (isNewRound) {
            // 1. 被格檔
            if (isParried) {
                img.src = 'image/' + actionToState(action) + '.png';
                setTimeout(() => { img.src = 'image/guard_break.png'; }, 200);
                return;
            }
            // 2. 成功格檔
            if (isParrying) {
                setTimeout(() => { img.src = 'image/parry.png'; }, 200);
                return;
            }
            // 3. 被擊中 (受擊方)
            if (isHit) {
                img.src = 'image/' + actionToState(action) + '.png';
                // 延遲取決於對手是哪種攻擊：重擊 0.6s, 其他 0.2s
                const delay = (opponentState === 'H_atk' || opponentAction === 'H_ATK') ? 600 : 200;
                setTimeout(() => { img.src = 'image/get_hit.png'; }, delay);
                return;
            }
            // 4. 攻擊成功 (攻擊方)
            if (isAttacking) {
                if (newState === 'H_atk') {
                    img.src = 'image/H_ready.png';
                    setTimeout(() => { img.src = 'image/H_atk.png'; }, 600);
                } else {
                    setTimeout(() => { img.src = 'image/' + newState + '.png'; }, 200);
                }
                return;
            }
            // 5. 普通重擊 (沒打中人也沒被打中)
            if (newState === 'H_atk') {
                img.src = 'image/H_ready.png';
                setTimeout(() => { img.src = 'image/H_atk.png'; }, 600);
                return;
            }
        }
        
        // 預設更新
        const currentSrc = img.src;
        const isAnimating = currentSrc.indexOf('H_ready.png') !== -1 || 
                           currentSrc.indexOf('parry.png') !== -1 || 
                           currentSrc.indexOf('guard_break.png') !== -1 || 
                           currentSrc.indexOf('get_hit.png') !== -1 ||
                           currentSrc.indexOf('win.png') !== -1 || 
                           currentSrc.indexOf('lose.png') !== -1;

        if (!isAnimating || !isNewRound) {
             img.src = 'image/' + newState + '.png';
        }
    }

    function updatePage(data) {
        const wasFirstPoll = isFirstPoll;
        if (data.opponentLeft === 'true') {
            showModal("對手跑了", "他是俗辣", "<button onclick='leaveRoom()'>返回大廳</button>");
            if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
            return;
        }

        // 偵測遊戲重新開始 (再戰 Rematch)，重置 client 端的 roundCount 追蹤
        if (!data.gameOver && data.roundCount === 1 && lastRoundCount > 1) {
            lastRoundCount = 0;
        }

        const isNewRound = data.roundCount > lastRoundCount;
        const isGameOverFirstTime = data.gameOver && !gameOverHandled;

        if (isNewRound || isGameOverFirstTime) {
            const selfIsAttacking = (data.selfState === 'L_atk' || data.selfState === 'M_atk' || data.selfState === 'H_atk') && data.opponentState === 'get_hit';
            const opponentIsAttacking = (data.opponentState === 'L_atk' || data.opponentState === 'M_atk' || data.opponentState === 'H_atk') && data.selfState === 'get_hit';

            // 自我音效
            if (data.selfState !== 'idle' && data.selfState !== 'get_hit') {
                if (!(data.selfState === 'parry' && data.selfStunned)) {
                    let delay = 0;
                    if (data.selfState === 'H_atk') delay = 600;
                    else if (data.selfState === 'parry' && !data.selfStunned) delay = 200;
                    else if (selfIsAttacking) delay = 200; 
                    playSound(data.selfState, delay);
                }
            }
            // 對手音效
            if (data.opponentState !== 'idle' && data.opponentState !== 'get_hit') {
                if (!(data.opponentState === 'parry' && data.opponentStunned)) {
                    let delay = 0;
                    if (data.opponentState === 'H_atk') delay = 600;
                    else if (data.opponentState === 'parry' && !data.opponentStunned) delay = 200;
                    else if (opponentIsAttacking) delay = 200;
                    playSound(data.opponentState, delay);
                }
            }
            // 雙方對撞音效
            if (data.selfState === 'get_hit' && data.opponentState === 'get_hit') {
                const selfDelay = (data.opponentAction === 'H_ATK') ? 600 : 200;
                const oppDelay = (data.selfAction === 'H_ATK') ? 600 : 200;
                playSound(actionToState(data.selfAction), selfDelay);
                playSound(actionToState(data.opponentAction), oppDelay);
            }

            lastRoundCount = data.roundCount;
        }

        if (isNewRound && !data.gameOver) {
            if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
            turnSeconds = 5;
        }

        document.getElementById('selfBattleName').textContent = data.selfName;
        document.getElementById('opponentBattleName').textContent = data.opponentName;
        document.getElementById('roundCount').textContent = data.roundCount;
        document.getElementById('lastMessage').textContent = data.lastMessage;

        const isFirstPollOfResolvedRound = isNewRound || isGameOverFirstTime;

        if (data.roomStage === 'waiting') {
            // Clear details
            document.getElementById('selfCounterText').textContent = "";
            document.getElementById('selfActionName').textContent = "";
            document.getElementById('selfActionName').style.display = 'none';

            document.getElementById('opponentCounterText').textContent = "";
            document.getElementById('opponentActionName').textContent = "";
            document.getElementById('opponentActionName').style.display = 'none';

            document.getElementById('parryWatermark').style.display = 'none';
        } else if (isFirstPollOfResolvedRound || wasFirstPoll) {
            const selfAction = data.selfAction;
            const opponentAction = data.opponentAction;
            const selfState = data.selfState;
            const opponentState = data.opponentState;

            // Determine if we should display details
            const showDetails = !(data.roundCount === 1 && selfAction === 'NONE' && opponentAction === 'NONE');

            if (showDetails) {
                // Update action names
                document.getElementById('selfActionName').textContent = getActionChineseName(selfAction);
                document.getElementById('opponentActionName').textContent = getActionChineseName(opponentAction);
                document.getElementById('selfActionName').style.display = 'inline-block';
                document.getElementById('opponentActionName').style.display = 'inline-block';

                // Check for counter
                let selfCounter = "";
                let opponentCounter = "";

                if (isAttack(selfAction) && isAttack(opponentAction)) {
                    if (selfState !== 'get_hit' && opponentState === 'get_hit') {
                        selfCounter = "COUNTER";
                    } else if (opponentState !== 'get_hit' && selfState === 'get_hit') {
                        opponentCounter = "COUNTER";
                    }
                }

                // Check for guard break (破防)
                if (isAttack(selfAction) && opponentAction === 'GUARD' && opponentState === 'guard_break') {
                    selfCounter = "GUARD BREAKING";
                }
                if (isAttack(opponentAction) && selfAction === 'GUARD' && selfState === 'guard_break') {
                    opponentCounter = "GUARD BREAKING";
                }

                document.getElementById('selfCounterText').textContent = selfCounter;
                document.getElementById('opponentCounterText').textContent = opponentCounter;

                // Parry Watermark
                const isParryOccurred = (selfState === 'parry' && opponentState === 'guard_break') || 
                                       (opponentState === 'parry' && selfState === 'guard_break');
                if (isParryOccurred) {
                    document.getElementById('parryWatermark').style.display = 'block';
                } else {
                    document.getElementById('parryWatermark').style.display = 'none';
                }
            } else {
                // Clear details
                document.getElementById('selfCounterText').textContent = "";
                document.getElementById('selfActionName').textContent = "";
                document.getElementById('selfActionName').style.display = 'none';

                document.getElementById('opponentCounterText').textContent = "";
                document.getElementById('opponentActionName').textContent = "";
                document.getElementById('opponentActionName').style.display = 'none';

                document.getElementById('parryWatermark').style.display = 'none';
            }
        }

        const selfHpPercent = data.selfMaxHp ? (data.selfHp / data.selfMaxHp) * 100 : 0;
        const opponentHpPercent = data.opponentMaxHp ? (data.opponentHp / data.opponentMaxHp) * 100 : 0;
        document.getElementById('selfHpBar').style.width = selfHpPercent + '%';
        document.getElementById('opponentHpBar').style.width = opponentHpPercent + '%';

        const selfStancePercent = data.selfMaxStance ? (data.selfStance / data.selfMaxStance) * 100 : 0;
        const opponentStancePercent = data.opponentMaxStance ? (data.opponentMaxStance / data.opponentMaxStance) * 100 : 0;
        document.getElementById('selfStanceBar').style.width = selfStancePercent + '%';
        document.getElementById('opponentStanceBar').style.width = opponentStancePercent + '%';

        const selfParrying = data.selfState === 'parry' && !data.selfStunned;
        const opponentParrying = data.opponentState === 'parry' && !data.opponentStunned;
        const selfParried = opponentParrying && data.selfState === 'guard_break';
        const opponentParried = selfParrying && data.opponentState === 'guard_break';

        if (data.roomStage === 'waiting') {
            document.getElementById('roomStatus').textContent = '等待對手加入...';
            disableButtons(true);
            document.getElementById('submitHint').textContent = '請等待對手加入並開始遊戲。';
            if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
            updateTimerBox("WAIT");
            document.getElementById('selfImg').src = 'image/idle.png';
            document.getElementById('opponentImg').src = 'image/idle.png';
        } else if (data.gameOver) {
            document.getElementById('roomStatus').textContent = '遊戲結束';
            disableButtons(true);
            document.getElementById('submitHint').textContent = '遊戲結束。等待選擇再戰或離開。';
            if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
            updateTimerBox("GG");
            
            if (!resultShown) {
                updateCharacterImg('selfImg', data.selfState, data.selfAction, isNewRound, selfParried, selfParrying, data.opponentState, data.opponentAction);
                updateCharacterImg('opponentImg', data.opponentState, data.opponentAction, isNewRound, opponentParried, opponentParrying, data.selfState, data.selfAction);
            }

            if (!gameOverHandled) {
                gameOverHandled = true;
                // 遊戲結束時停止並重置背景音樂
                bgm.pause();
                bgm.currentTime = 0;
                bgmStarted = false;

                setTimeout(() => {
                    resultShown = true;
                    if (data.selfHp > data.opponentHp) {
                        document.getElementById('selfImg').src = 'image/win.png';
                        document.getElementById('opponentImg').src = 'image/lose.png';
                        playSound('you_win');
                        showModal("你贏了", "是否要再來一局？", "<button onclick='requestRematch()'>再戰</button><button onclick='leaveRoom()'>退出</button>");
                    } else if (data.opponentHp > data.selfHp) {
                        document.getElementById('selfImg').src = 'image/lose.png';
                        document.getElementById('opponentImg').src = 'image/win.png';
                        playSound('you_lose');
                        showModal("服不服", "要不要報仇？", "<button onclick='requestRematch()'>不服(再戰)</button><button onclick='leaveRoom()'>服(退出)</button>");
                    } else {
                        showModal("平手", "平分秋色，再來一局？", "<button onclick='requestRematch()'>再戰</button><button onclick='leaveRoom()'>退出</button>");
                    }
                }, 2500);
            } else if (resultShown) {
                if (data.selfRematchRequested) {
                    showModal("等待中", "等待對手回應再戰請求...", "<button onclick='leaveRoom()'>取消並退出</button>");
                }
            }
        } else {
            gameOverHandled = false;
            resultShown = false;
            hideModal();
            
            // 遊戲進行中若背景音樂尚未播放，自動啟動/重啟背景音樂
            if (data.roomStage === 'playing' && !bgmStarted) {
                startBGM();
            }
            
            updateCharacterImg('selfImg', data.selfState, data.selfAction, isNewRound, selfParried, selfParrying, data.opponentState, data.opponentAction);
            updateCharacterImg('opponentImg', data.opponentState, data.opponentAction, isNewRound, opponentParried, opponentParrying, data.selfState, data.selfAction);

            document.getElementById('roomStatus').textContent = data.selfSubmitted ? '已選擇，等待對手' : '請選擇你的動作';
            disableButtons(!data.canSubmit);

            if (data.canSubmit) {
                if (data.selfStunned) {
                    if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
                    updateTimerBox("暈");
                    submitAction('NONE');
                } else {
                    if (!turnTimer) {
                        turnTimer = setInterval(() => {
                            turnSeconds--;
                            if (turnSeconds <= 0) {
                                clearInterval(turnTimer);
                                turnTimer = null;
                                submitAction('NONE');
                            } else {
                                updateHintWithTimer(turnSeconds);
                            }
                        }, 1000);
                    }
                    updateHintWithTimer(turnSeconds);
                }
            } else {
                if (turnTimer) { clearInterval(turnTimer); turnTimer = null; }
                document.getElementById('submitHint').textContent = '已送出動作，等待對手...';
                updateTimerBox("⌛");
            }
        }

        lastSelfHp = data.selfHp;
        lastOpponentHp = data.opponentHp;
        isFirstPoll = false;
    }

    function updateHintWithTimer(sec) {
        document.getElementById('submitHint').textContent = `請於 ${sec} 秒內選擇動作，否則將自動跳過。`;
        updateTimerBox(sec);
    }

    function updateTimerBox(val) {
        document.getElementById('battleTimerBox').textContent = val;
    }

    function disableButtons(disabled) {
        document.querySelectorAll('.buttons-grid button:not(.empty)').forEach(btn => {
            btn.disabled = disabled;
        });
    }

    function disableAllButSkip() {
        document.querySelectorAll('.buttons-grid button:not(.empty)').forEach(btn => {
            if (btn.id === 'skipButton') {
                btn.disabled = false;
            } else {
                btn.disabled = true;
            }
        });
    }

    fetchState();
    setInterval(fetchState, refreshInterval);
</script>
</body>
</html>

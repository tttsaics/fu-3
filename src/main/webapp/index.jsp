<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-Hant">
<head>
    <meta charset="UTF-8">
    <title>ARMOR - JSP 對戰大廳</title>
    <style>
        /* Sketch Style Matching Battle Interface */
        body {
            margin: 0;
            padding: 0;
            background: #ffffff; /* 白底天空 */
            color: #000000;
            font-family: "Microsoft JhengHei", sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            position: relative;
        }

        /* Gray Floor Divider matching Battle Scene */
        body::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 0;
            width: 100%;
            height: 220px; /* 地面高度 */
            background: #f4f4f4;
            border-top: 4px solid #333333;
            z-index: 1; /* 位於白底之上，但低於 UI */
        }

        .lobby-container {
            position: relative;
            z-index: 10;
            display: flex;
            flex-direction: column;
            align-items: center;
            width: 100%;
            max-width: 480px;
            padding: 20px;
            padding-bottom: 240px; /* 留出底部地板空間 */
            box-sizing: border-box;
        }

        .logo-container {
            margin-bottom: 30px;
            text-align: center;
        }

        .lobby-title {
            font-family: "Impact", sans-serif;
            font-weight: bold;
            font-size: 72px;
            letter-spacing: 0.1em;
            margin: 0;
            color: #000000;
        }

        .lobby-subtitle {
            font-size: 18px;
            font-weight: bold;
            color: #666666;
            margin-top: 5px;
            letter-spacing: 2px;
        }

        /* Menu card matching Battle Panel */
        .menu-card {
            width: 100%;
            background: rgba(0, 0, 0, 0.9);
            border: 4px solid #333333;
            border-radius: 16px 16px 0 0; /* 圓角上方 */
            padding: 30px;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.25);
            box-sizing: border-box;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .menu-btn {
            font-family: "Microsoft JhengHei", sans-serif;
            font-size: 18px;
            font-weight: bold;
            padding: 14px;
            background: transparent;
            color: #ffffff;
            border: 2px solid rgba(255, 255, 255, 0.7);
            border-radius: 6px;
            cursor: pointer;
            transition: background 0.15s, border-color 0.15s;
            outline: none;
        }

        .menu-btn:hover {
            background: rgba(255, 255, 255, 0.15);
            border-color: #ffffff;
        }

        .btn-cancel {
            border-color: #ff3b30;
            color: #ff3b30;
        }
        .btn-cancel:hover {
            background: rgba(255, 59, 48, 0.15);
            border-color: #ff453a;
            color: #ff453a;
        }

        .footer-text {
            margin-top: 40px;
            font-size: 12px;
            font-weight: bold;
            color: #333333;
            letter-spacing: 1px;
            z-index: 10;
        }

        /* Modal GUI styles matching game.jsp overlay */
        .modal-overlay {
            display: none;
            position: fixed;
            top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0, 0, 0, 0.7);
            z-index: 1000;
            justify-content: center;
            align-items: center;
        }

        .modal-content {
            background: #222222;
            color: #ffffff;
            padding: 30px;
            border-radius: 12px;
            border: 2px solid #555555;
            text-align: center;
            max-width: 400px;
            width: 90%;
            box-shadow: 0 10px 30px rgba(0,0,0,0.8);
            box-sizing: border-box;
        }

        .modal-content h2 {
            margin-top: 0;
            font-size: 24px;
            font-weight: bold;
            margin-bottom: 20px;
        }

        .modal-content label {
            display: block;
            font-size: 14px;
            color: #aaaaaa;
            margin-bottom: 8px;
            text-align: left;
            font-weight: bold;
        }

        .modal-input {
            background: rgba(0, 0, 0, 0.4);
            border: 2px solid rgba(255, 255, 255, 0.3);
            border-radius: 6px;
            color: #ffffff;
            font-family: "Microsoft JhengHei", sans-serif;
            font-size: 16px;
            padding: 12px;
            width: 100%;
            box-sizing: border-box;
            margin-bottom: 20px;
            outline: none;
        }

        .modal-input:focus {
            border-color: #ffffff;
        }

        .modal-btn-group {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .room-list-container {
            border: 2px solid rgba(255, 255, 255, 0.3);
            border-radius: 6px;
            padding: 10px;
            max-height: 150px;
            overflow-y: auto;
            background: rgba(0,0,0,0.4);
            display: flex;
            flex-direction: column;
            gap: 8px;
            margin-top: 8px;
        }

        .room-item-btn {
            font-family: "Microsoft JhengHei", sans-serif;
            width: 100%;
            background: transparent;
            border: 1px solid rgba(255, 255, 255, 0.5);
            border-radius: 4px;
            color: #ffffff;
            padding: 8px 12px;
            text-align: left;
            cursor: pointer;
            font-size: 14px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-sizing: border-box;
            transition: background 0.15s, border-color 0.15s;
        }

        .room-item-btn:hover {
            background: rgba(255, 255, 255, 0.15);
            border-color: #ffffff;
        }
    </style>
</head>
<body>
<div class="lobby-container">
    <div class="logo-container">
        <h1 class="lobby-title">ARMOR</h1>
        <div class="lobby-subtitle">雙人連線對戰</div>
    </div>

    <div class="menu-card">
        <button class="menu-btn" onclick="openSingleplayer()">單人遊戲</button>
        <button class="menu-btn" onclick="openMultiplayer()">多人遊戲</button>
    </div>

    <div class="footer-text">
        ARMOR CLASH - JSP EDITION v1.0
    </div>
</div>

<!-- Singleplayer Modal -->
<div class="modal-overlay" id="singleplayerModal">
    <div class="modal-content">
        <h2>單人電腦對戰</h2>
        <form action="game" method="post">
            <input type="hidden" name="action" value="create_computer" />
            
            <label for="spName">玩家名稱</label>
            <input type="text" id="spName" name="name" class="modal-input" maxlength="20" required placeholder="輸入你的暱稱..." autocomplete="off" />

            <div class="modal-btn-group">
                <button type="submit" class="menu-btn">開始對戰</button>
                <button type="button" class="menu-btn btn-cancel" onclick="closeModal()">取消</button>
            </div>
        </form>
    </div>
</div>

<!-- Multiplayer Modal -->
<div class="modal-overlay" id="multiplayerModal">
    <div class="modal-content">
        <h2>多人連線對戰</h2>
        <form id="mpForm" action="game" method="post">
            <input type="hidden" id="mpAction" name="action" value="create" />
            
            <label for="mpName">玩家名稱</label>
            <input type="text" id="mpName" name="name" class="modal-input" maxlength="20" required placeholder="輸入你的暱稱..." autocomplete="off" />

            <!-- Room list section -->
            <div id="roomListSection" style="margin-bottom: 20px; text-align: left;">
                <label>可加入的房間列表</label>
                <div id="roomListContainer" class="room-list-container">
                    <div style="color: #888; text-align: center; padding: 10px 0;">載入中...</div>
                </div>
            </div>

            <!-- Room code section (hidden initially) -->
            <div id="mpRoomCodeSection" style="display: none;">
                <label for="mpRoomCode">房間代碼</label>
                <input type="text" id="mpRoomCode" name="roomCode" class="glass-input modal-input" maxlength="4" placeholder="請輸入 4 位數代碼..." autocomplete="off" />
            </div>

            <div class="modal-btn-group" id="mpInitialButtons">
                <button type="button" class="menu-btn" onclick="submitMpCreate()">建立新房間</button>
                <button type="button" class="menu-btn" onclick="showMpJoinFields()">加入現有房間</button>
                <button type="button" class="menu-btn btn-cancel" onclick="closeModal()">取消</button>
            </div>

            <div class="modal-btn-group" id="mpJoinButtons" style="display: none;">
                <button type="submit" class="menu-btn">確認加入</button>
                <button type="button" class="menu-btn btn-cancel" onclick="showMpInitialButtons()">返回</button>
            </div>
        </form>
    </div>
</div>

<script>
    let roomListInterval = null;

    async function loadRoomList() {
        const container = document.getElementById('roomListContainer');
        try {
            const response = await fetch('game?action=list_rooms');
            if (!response.ok) throw new Error();
            const data = await response.json();
            if (data.success && data.rooms && data.rooms.length > 0) {
                container.innerHTML = '';
                data.rooms.forEach(room => {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'room-item-btn';
                    btn.innerHTML = '<span>房間代碼: <strong>' + room.roomCode + '</strong></span><span>房主: <strong>' + room.hostName + '</strong></span>';
                    btn.onclick = () => selectRoomToJoin(room.roomCode);
                    container.appendChild(btn);
                });
            } else {
                container.innerHTML = '<div style="color: #888; text-align: center; padding: 10px 0;">目前沒有可加入的房間。</div>';
            }
        } catch (e) {
            container.innerHTML = '<div style="color: #ff3b30; text-align: center; padding: 10px 0;">載入房間列表失敗。</div>';
        }
    }

    function selectRoomToJoin(roomCode) {
        const nameInput = document.getElementById('mpName');
        if (!nameInput.checkValidity()) {
            nameInput.reportValidity();
            return;
        }
        document.getElementById('mpAction').value = 'join';
        
        let roomCodeInput = document.getElementById('mpRoomCode');
        if (!roomCodeInput) {
            roomCodeInput = document.createElement('input');
            roomCodeInput.type = 'hidden';
            roomCodeInput.name = 'roomCode';
            roomCodeInput.id = 'mpRoomCode';
            document.getElementById('mpForm').appendChild(roomCodeInput);
        }
        roomCodeInput.value = roomCode;
        
        document.getElementById('mpForm').submit();
    }

    function openSingleplayer() {
        document.getElementById('singleplayerModal').style.display = 'flex';
        document.getElementById('spName').focus();
    }

    function openMultiplayer() {
        document.getElementById('multiplayerModal').style.display = 'flex';
        document.getElementById('mpName').focus();
        loadRoomList();
        if (roomListInterval) clearInterval(roomListInterval);
        roomListInterval = setInterval(loadRoomList, 3000);
    }

    function closeModal() {
        document.getElementById('singleplayerModal').style.display = 'none';
        document.getElementById('multiplayerModal').style.display = 'none';
        showMpInitialButtons();
        if (roomListInterval) {
            clearInterval(roomListInterval);
            roomListInterval = null;
        }
    }

    function submitMpCreate() {
        const nameInput = document.getElementById('mpName');
        if (nameInput.checkValidity()) {
            document.getElementById('mpAction').value = 'create';
            document.getElementById('mpForm').submit();
        } else {
            nameInput.reportValidity();
        }
    }

    function showMpJoinFields() {
        const nameInput = document.getElementById('mpName');
        if (!nameInput.checkValidity()) {
            nameInput.reportValidity();
            return;
        }
        document.getElementById('mpAction').value = 'join';
        document.getElementById('mpRoomCodeSection').style.display = 'block';
        document.getElementById('mpRoomCode').required = true;
        document.getElementById('mpInitialButtons').style.display = 'none';
        document.getElementById('mpJoinButtons').style.display = 'flex';
        document.getElementById('mpRoomCode').focus();
    }

    function showMpInitialButtons() {
        document.getElementById('mpRoomCodeSection').style.display = 'none';
        document.getElementById('mpRoomCode').required = false;
        document.getElementById('mpRoomCode').value = '';
        document.getElementById('mpInitialButtons').style.display = 'flex';
        document.getElementById('mpJoinButtons').style.display = 'none';
    }
</script>
</body>
</html>

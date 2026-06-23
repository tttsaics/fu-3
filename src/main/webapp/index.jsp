<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-Hant">
<head>
    <meta charset="UTF-8">
    <title>ARMOR - JSP 對戰大廳</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="page-container">
    <header>
        <h1>ARMOR</h1>
        <p>JSP 雙人連線版</p>
    </header>

    <div class="content-box">
        <section class="box">
            <h2>建立房間</h2>
            <form action="game" method="post">
                <input type="hidden" name="action" value="create" />
                <label>玩家名稱<br>
                    <input type="text" name="name" maxlength="20" required>
                </label>
                <button type="submit">建立房間</button>
            </form>
        </section>

        <section class="box">
            <h2>加入房間</h2>
            <form action="game" method="post">
                <input type="hidden" name="action" value="join" />
                <label>玩家名稱<br>
                    <input type="text" name="name" maxlength="20" required>
                </label>
                <label>房間代碼<br>
                    <input type="text" name="roomCode" maxlength="4" required>
                </label>
                <button type="submit">加入對戰</button>
            </form>
        </section>

        <section class="box">
            <h2>電腦對戰</h2>
            <form action="game" method="post">
                <input type="hidden" name="action" value="create_computer" />
                <label>玩家名稱<br>
                    <input type="text" name="name" maxlength="20" required>
                </label>
                <button type="submit">開始電腦對戰</button>
            </form>
        </section>
    </div>
</div>
</body>
</html>

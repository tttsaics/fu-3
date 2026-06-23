<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-Hant">
<head>
    <meta charset="UTF-8">
    <title>ARMOR - 錯誤</title>
    <link rel="stylesheet" href="css/style.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="page-container">
    <header>
        <h1>ARMOR</h1>
        <p>發生錯誤</p>
    </header>

    <div class="content-box">
        <section class="box">
            <p class="message"><%= request.getAttribute("message") != null ? request.getAttribute("message") : "發生未知錯誤。" %></p>
            <a class="button" href="index.jsp">回到首頁</a>
        </section>
    </div>
</div>
</body>
</html>

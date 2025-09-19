<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.lang.Boolean" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <title>Dodaj novog partnera</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 2em; }
        form { max-width: 600px; }
        label { display: block; margin-top: 1em; font-weight: bold; }
        input, select, textarea { width: 100%; padding: 0.5em; margin-top: 0.2em; }
        .message { margin: 1em 0; padding: 0.5em; border-radius: 4px; }
        .success { background-color: #e0f7e9; color: #2b7a3a; border: 1px solid #2b7a3a; }
        .error { background-color: #fbeaea; color: #a33a3a; border: 1px solid #a33a3a; }
    </style>
</head>
<body>
<h1>Dodaj novog partnera</h1>

<%
    String poruka = (String) request.getAttribute("poruka");
    Boolean pogreska = (Boolean) request.getAttribute("pogreska");
    if (poruka != null) {
%>
<div class="message <%= (pogreska != null && pogreska) ? "error" : "success" %>">
    <%= poruka %>
</div>
<%
    }
%>

<form action='<%= request.getContextPath() %>/mvc/tvrtka/partner/dodaj' method='post'>

    <label for="naziv">ID partnera:</label>
    <input type="text" id="id" name="id" value='<%= request.getAttribute("id") %>' required />

    <label for="naziv">Naziv partnera:</label>
    <input type="text" id="naziv" name="naziv" value='<%= request.getAttribute("naziv") %>' required />

    <label for="vrstaKuhinje">Vrsta kuhinje:</label>
    <input type="text" id="vrstaKuhinje" name="vrstaKuhinje" value='<%= request.getAttribute("vrstaKuhinje") %>' required />

    <label for="adresa">Adresa:</label>
    <textarea id="adresa" name="adresa" rows="2" required><%= request.getAttribute("adresa") %></textarea>

    <label for="mreznaVrata">Mrežna vrata (početak):</label>
    <input type="number" id="mreznaVrata" name="mreznaVrata" value='<%= request.getAttribute("mreznaVrata") %>' required />

    <label for="mreznaVrataKraj">Mrežna vrata (kraj):</label>
    <input type="number" id="mreznaVrataKraj" name="mreznaVrataKraj" value='<%= request.getAttribute("mreznaVrataKraj") %>' required />

    <label for="gpsSirina">GPS širina (lat):</label>
    <input type="number" step="any" id="gpsSirina" name="gpsSirina" value='<%= request.getAttribute("gpsSirina") %>' required />

    <label for="gpsDuzina">GPS dužina (lon):</label>
    <input type="number" step="any" id="gpsDuzina" name="gpsDuzina" value='<%= request.getAttribute("gpsDuzina") %>' required />

    <label for="sigurnosniKod">Sigurnosni kod:</label>
    <input type="text" id="sigurnosniKod" name="sigurnosniKod" value='<%= request.getAttribute("sigurnosniKod") %>' required />

    <label for="adminKod">Admin kod:</label>
    <input type="password" id="adminKod" name="adminKod" value='<%= request.getAttribute("adminKod") %>' required />

    <button type="submit" style="margin-top: 1.5em; padding: 0.7em 1.5em;">Spremi partnera</button>
</form>

</body>
</html>

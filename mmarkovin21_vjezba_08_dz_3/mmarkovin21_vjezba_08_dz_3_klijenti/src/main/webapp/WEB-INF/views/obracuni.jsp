<%@ page import="edu.unizg.foi.nwtis.podaci.Obracun" %>
<%@ page import="java.util.List" %>
<%@ page import="edu.unizg.foi.nwtis.podaci.Partner" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Obračuni</title>
</head>
<body>

<h2>Pretraži obračune</h2>

<form method="get" action="">
    <label for="tip">Odaberi tip obračuna:</label>
    <select name="tip" id="tip">
        <option value="jelo">Jelo</option>
        <option value="pice">Piće</option>
        <option value="jelo i pice">Jelo i piće</option>
    </select>
    <br/><br/>

    <label for="od">Od datuma:</label>
    <input type="date" id="od" name="od" value="<%= request.getParameter("od") != null ? request.getParameter("od") : "" %>">
    <br/><br/>

    <label for="do">Do datuma:</label>
    <input type="date" id="do" name="do" value="<%= request.getParameter("do") != null ? request.getParameter("do") : "" %>">
    <br/><br/>

    <label for="partner">Odaberi partnera:</label>
    <select name="partner" id="partner">
        <option value="">Bez partnera</option>
        <%
            List<Partner> partneri = (List<Partner>) request.getAttribute("partneri");
            if (partneri != null) {
                for (Partner p : partneri) {
        %>
        <option value="<%= p.id() %>"><%= p.naziv() %></option>
        <%
                }
            }
        %>
    </select>

    <br/><br/>
    <button type="submit">Pretraži</button>
</form>

<script>

    const ctx = '<%= request.getContextPath() %>';
    const mvcBase = ctx + '/mvc/tvrtka';
    document.querySelector('form').addEventListener('submit', function(e) {
        e.preventDefault();

        const tip       = document.getElementById('tip').value;
        const odStr     = document.getElementById('od').value;
        const doStr     = document.getElementById('do').value;
        const partnerId = document.getElementById('partner').value;

        let path = mvcBase + '/obracun';

        if (partnerId) {
            path += '/' + partnerId;
        }
        else if (tip === 'jelo')   path += '/jelo';
        else if (tip === 'pice')    path += '/pice';

        let qs = [];
        if (odStr) {
            const odMillis = new Date(odStr).getTime();
            qs.push('od=' + odMillis);
        }
        if (doStr) {
            const doMillis = new Date(doStr).getTime();
            qs.push('do=' + doMillis);
        }
        if (qs.length) path += '?' + qs.join('&');

        window.location.href = path;
    });
</script>


<%
    List<Obracun> obracuni = (List<Obracun>) request.getAttribute("obracuni");
    if (obracuni != null && !obracuni.isEmpty()) {
%>
<h3>Rezultati obračuna</h3>
<table>
    <tr>
        <th>Partner</th>
        <th>ID kuhinje/pica</th>
        <th>Jeli jelo</th>
        <th>Količina</th>
        <th>Cijena</th>
        <th>Datum</th>
    </tr>
    <% for (Obracun obracun : obracuni) { %>
    <tr>
        <td><%= obracun.partner() %></td>
        <td><%= obracun.id() %></td>
        <td><%= obracun.jelo() %></td>
        <td><%= obracun.kolicina() %></td>
        <td><%= obracun.cijena() %></td>
        <td><%= obracun.vrijeme() %></td>
    </tr>
    <% } %>
</table>
<%
    }
%>
</body>
</html>

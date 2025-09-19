<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="edu.unizg.foi.nwtis.podaci.Partner" %>
<%
    Partner partner = (Partner) request.getAttribute("partner");
%>
<html>
<head>
    <title>Detalji partnera</title>
</head>
    <body>
        <h2>Detalji partnera</h2>
        <%
            if (partner != null) {
        %>
        <p><strong>ID:</strong> <%= partner.id() %></p>
        <p><strong>Naziv:</strong> <%= partner.naziv() %></p>
        <p><strong>Kontakt:</strong> <%= partner.adresa() %></p>
        <p><strong>Adresa:</strong> <%= partner.vrstaKuhinje() %></p>
        <p><strong>GPS širina:</strong> <%= partner.gpsSirina() %></p>
        <p><strong>GPS dužina:</strong> <%= partner.gpsDuzina() %></p>
        <p><strong>Adresa mrežnih vrata:</strong> <%= partner.mreznaVrata() %></p>
        <p><strong>Adresa mrežnih vrata za kraj:</strong> <%= partner.mreznaVrataKraj() %></p>
        <p><strong>Admin kod:</strong> <%= partner.adminKod() %></p>
        <%
        } else {
        %>
        <p style="color:red">Greška: Partner nije pronađen.</p>
        <%
            }
        %>
    </body>
</html>


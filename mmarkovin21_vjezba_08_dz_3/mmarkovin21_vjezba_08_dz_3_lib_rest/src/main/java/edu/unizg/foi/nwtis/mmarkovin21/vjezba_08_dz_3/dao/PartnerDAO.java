package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3.dao;

import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object za entitet Partner i Obracun u bazi podataka.
 * Pruža metode za dohvat, unos, ažuriranje i filtriranje podataka.
 *
 * @author Dragutin Kermek
 */
public class PartnerDAO {

  /** Veza prema bazi podataka. */
  private Connection vezaBP;

  /**
   * Konstruktor koji inicijalizira DAO s danom baznom vezom.
   *
   * @param vezaBP aktivna veza prema bazi podataka
   */
  public PartnerDAO(Connection vezaBP) {
    super();
    this.vezaBP = vezaBP;
  }

  /**
   * Dohvati partnera prema ID-ju.
   *
   * @param id           jedinstveni identifikator partnera
   * @param sakriKodove  ako je true, uklanja sigurnosni i administratorski kod iz rezultata
   * @return objekt Partner ili null ako ne postoji ili došlo do pogreške
   */
  public Partner dohvati(int id, boolean sakriKodove) {
    String upit = "SELECT naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod FROM partneri WHERE id = ?";
    try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
      s.setInt(1, id);
      ResultSet rs = s.executeQuery();
      if (rs.next()) {
        Partner p = new Partner(
                id,
                rs.getString("naziv"),
                rs.getString("vrstaKuhinje"),
                rs.getString("adresa"),
                rs.getInt("mreznaVrata"),
                rs.getInt("mreznaVrataKraj"),
                rs.getFloat("gpsSirina"),
                rs.getFloat("gpsDuzina"),
                rs.getString("sigurnosniKod"),
                rs.getString("adminKod")
        );
        return sakriKodove ? p.partnerBezKodova() : p;
      }
    } catch (SQLException ex) {
      Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Dohvati partnera prema vrsti kuhinje.
   *
   * @param idKuhinje    oznaka vrste kuhinje
   * @param sakriKodove  ako je true, uklanja sigurnosni i administratorski kod iz rezultata
   * @return objekt Partner ili null ako ne postoji ili došlo do pogreške
   */
  public Partner dohvatiKuhinju(String idKuhinje, boolean sakriKodove) {
    String upit = "SELECT id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod FROM partneri WHERE vrstaKuhinje = ?";
    try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
      s.setString(1, idKuhinje);
      ResultSet rs = s.executeQuery();
      if (rs.next()) {
        Partner p = new Partner(
                rs.getInt("id"),
                rs.getString("naziv"),
                rs.getString("vrstaKuhinje"),
                rs.getString("adresa"),
                rs.getInt("mreznaVrata"),
                rs.getInt("mreznaVrataKraj"),
                rs.getFloat("gpsSirina"),
                rs.getFloat("gpsDuzina"),
                rs.getString("sigurnosniKod"),
                rs.getString("adminKod")
        );
        return sakriKodove ? p.partnerBezKodova() : p;
      }
    } catch (SQLException ex) {
      Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Dohvati prvog partnera iz tablice.
   *
   * @param sakriKodove ako je true, uklanja sigurnosni i administratorski kod iz rezultata
   * @return prvi objekt Partner ili null ako tablica prazna ili dođe do pogreške
   */
  public Partner dohvatiPrvog(boolean sakriKodove) {
    String upit = "SELECT id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod FROM partneri LIMIT 1";
    try (Statement s = this.vezaBP.createStatement(); ResultSet rs = s.executeQuery(upit)) {
      if (rs.next()) {
        Partner p = new Partner(
                rs.getInt("id"),
                rs.getString("naziv"),
                rs.getString("vrstaKuhinje"),
                rs.getString("adresa"),
                rs.getInt("mreznaVrata"),
                rs.getInt("mreznaVrataKraj"),
                rs.getFloat("gpsSirina"),
                rs.getFloat("gpsDuzina"),
                rs.getString("sigurnosniKod"),
                rs.getString("adminKod")
        );
        return sakriKodove ? p.partnerBezKodova() : p;
      }
    } catch (SQLException ex) {
      Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Dohvati sve partnere.
   *
   * @param sakriKodove ako je true, uklanja kodove iz rezultata
   * @return lista objekata Partner ili null u slučaju pogreške
   */
  public List<Partner> dohvatiSve(boolean sakriKodove) {
    String upit = "SELECT id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod FROM partneri ORDER BY id";
    List<Partner> partneri = new ArrayList<>();
    try (Statement s = this.vezaBP.createStatement(); ResultSet rs = s.executeQuery(upit)) {
      while (rs.next()) {
        Partner p = new Partner(
                rs.getInt("id"),
                rs.getString("naziv"),
                rs.getString("vrstaKuhinje"),
                rs.getString("adresa"),
                rs.getInt("mreznaVrata"),
                rs.getInt("mreznaVrataKraj"),
                rs.getFloat("gpsSirina"),
                rs.getFloat("gpsDuzina"),
                rs.getString("sigurnosniKod"),
                rs.getString("adminKod")
        );
        partneri.add(sakriKodove ? p.partnerBezKodova() : p);
      }
      return partneri;
    } catch (SQLException ex) {
      Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Dodaj novi zapis partnera.
   *
   * @param p objekt Partner koji se unosi
   * @return true ako je unos uspješan, false inače
   */
  public boolean dodaj(Partner p) {
    String upit = "INSERT INTO partneri (id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
      s.setInt(1, p.id());
      s.setString(2, p.naziv());
      s.setString(3, p.vrstaKuhinje());
      s.setString(4, p.adresa());
      s.setInt(5, p.mreznaVrata());
      s.setInt(6, p.mreznaVrataKraj());
      s.setFloat(7, p.gpsSirina());
      s.setFloat(8, p.gpsDuzina());
      s.setString(9, p.sigurnosniKod());
      s.setString(10,p.adminKod());
      return s.executeUpdate() == 1;
    } catch (SQLException ex) {
      Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  /**
   * Dodaj novi zapis obračuna.
   *
   * @param o objekt Obracun za unos
   * @return true ako je unos uspješan, false inače
   */
  public boolean dodajObracun(Obracun o) {
    String upit = "INSERT INTO obracuni (partner, id, jelo, kolicina, cijena, vrijeme) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
      s.setInt(1, o.partner());
      s.setString(2, o.id());
      s.setBoolean(3,o.jelo());
      s.setFloat(4,o.kolicina());
      s.setFloat(5,o.cijena());
      LocalDateTime ldtUtc = LocalDateTime.ofInstant(Instant.ofEpochSecond(o.vrijeme()), ZoneOffset.UTC);
      s.setObject(6, ldtUtc);
      return s.executeUpdate() > 0;
    } catch (SQLException ex) {
      Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  /**
   * Dohvati obračune unutar vremenskog raspona.
   *
   * @param epochMillisOd početak raspona u milisekundama od epohe
   * @param epochMillisDo kraj raspona u milisekundama od epohe
   * @return lista objekata Obracun
   * @throws SQLException u slučaju pogreške pri radu s bazom
   */
  public List<Obracun> getObracuniOdDo(Long epochMillisOd, Long epochMillisDo) throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT partner,id,jelo,kolicina,cijena,vrijeme FROM obracuni");
    List<LocalDateTime> params = new ArrayList<>();
    if (epochMillisOd != null || epochMillisDo != null) {
      sql.append(" WHERE");
      boolean first = true;
      if (epochMillisOd != null) {
        sql.append(first?" ":" AND").append(" vrijeme >= ?");
        params.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillisOd), ZoneOffset.UTC));
        first = false;
      }
      if (epochMillisDo != null) {
        sql.append(first?" ":" AND").append(" vrijeme <= ?");
        params.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillisDo), ZoneOffset.UTC));
      }
    }
    try (PreparedStatement ps = vezaBP.prepareStatement(sql.toString())) {
      for (int i=0; i<params.size(); i++) ps.setObject(i+1, params.get(i));
      ResultSet rs = ps.executeQuery();
      List<Obracun> rezultat = new ArrayList<>();
      while (rs.next()) {
        long epochSec = rs.getTimestamp("vrijeme").toInstant().getEpochSecond();
        rezultat.add(new Obracun(rs.getInt("partner"), rs.getString("id"), rs.getBoolean("jelo"), rs.getFloat("kolicina"), rs.getFloat("cijena"), epochSec));
      }
      return rezultat;
    }
  }

  /**
   * Dohvati obračune za jela ili pića unutar raspona.
   *
   * @param jelo         true za jela, false za pića
   * @param epochMillisOd početak raspona u milisekundama
   * @param epochMillisDo kraj raspona u milisekundama
   * @return lista objekata Obracun
   * @throws SQLException u slučaju pogreške pri radu s bazom
   */
  public List<Obracun> getObracuniJeloILIPice(boolean jelo, Long epochMillisOd, Long epochMillisDo) throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT partner,id,jelo,kolicina,cijena,vrijeme FROM obracuni WHERE jelo=?");
    List<Object> params = new ArrayList<>();
    params.add(jelo);
    if (epochMillisOd != null) {
      sql.append(" AND vrijeme >= ?");
      params.add(new Timestamp(epochMillisOd));
    }
    if (epochMillisDo != null) {
      sql.append(" AND vrijeme <= ?");
      params.add(new Timestamp(epochMillisDo));
    }
    try (PreparedStatement ps = vezaBP.prepareStatement(sql.toString())) {
      for (int i=0; i<params.size(); i++) {
        Object p = params.get(i);
        if (p instanceof Timestamp) ps.setTimestamp(i+1, (Timestamp)p);
        else ps.setObject(i+1, p);
      }
      ResultSet rs = ps.executeQuery();
      List<Obracun> rezultat = new ArrayList<>();
      while (rs.next()) {
        long epochSec = rs.getTimestamp("vrijeme").toInstant().getEpochSecond();
        rezultat.add(new Obracun(rs.getInt("partner"), rs.getString("id"), rs.getBoolean("jelo"), rs.getFloat("kolicina"), rs.getFloat("cijena"), epochSec));
      }
      return rezultat;
    }
  }

  /**
   * Dohvati obračune po ID-ju i rasponu.
   *
   * @param id           ID partnera
   * @param epochMillisOd početak raspona
   * @param epochMillisDo kraj raspona
   * @return lista objekata Obracun
   * @throws SQLException u slučaju pogreške pri radu s bazom
   */
  public List<Obracun> getObracuniIdOdDo(int id, Long epochMillisOd, Long epochMillisDo) throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT partner,id,jelo,kolicina,cijena,vrijeme FROM obracuni WHERE id=?");
    List<Object> params = new ArrayList<>();
    params.add(id);
    if (epochMillisOd != null) {
      sql.append(" AND vrijeme >= ?");
      params.add(new Timestamp(epochMillisOd));
    }
    if (epochMillisDo != null) {
      sql.append(" AND vrijeme <= ?");
      params.add(new Timestamp(epochMillisDo));
    }
    try (PreparedStatement ps = vezaBP.prepareStatement(sql.toString())) {
      for (int i=0; i<params.size(); i++) {
        Object p = params.get(i);
        if (p instanceof Timestamp) ps.setTimestamp(i+1, (Timestamp)p);
        else ps.setObject(i+1, p);
      }
      ResultSet rs = ps.executeQuery();
      List<Obracun> rezultat = new ArrayList<>();
      while (rs.next()) {
        long epochSec = rs.getTimestamp("vrijeme").toInstant().getEpochSecond();
        rezultat.add(new Obracun(rs.getInt("partner"), rs.getString("id"), rs.getBoolean("jelo"), rs.getFloat("kolicina"), rs.getFloat("cijena"), epochSec));
      }
      return rezultat;
    }
  }
}
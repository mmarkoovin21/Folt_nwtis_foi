package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3.dao;

import edu.unizg.foi.nwtis.podaci.Korisnik;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object za rad s entitetom Korisnik u bazi podataka.
 * Pruža metode za dohvat, dohvat svih i unos korisnika.
 *
 * @author Dragutin Kermek
 */
public class KorisnikDAO {

  /** Veza prema bazi podataka. */
  private Connection vezaBP;

  /**
   * Konstruktor koji inicijalizira DAO s danom baznom vezom.
   *
   * @param vezaBP aktivna veza prema bazi podataka
   */
  public KorisnikDAO(Connection vezaBP) {
    super();
    this.vezaBP = vezaBP;
  }

  /**
   * Dohvati korisnika po korisničkom imenu, opcionalno provjeravajući lozinku.
   *
   * @param korisnik korisničko ime korisnika
   * @param lozinka lozinka korisnika (ako je prijava na true)
   * @param prijava  ako je true, metod provjerava i lozinku
   * @return objekt Korisnik ako postoji i (ako je prijava) lozinka je točna, inače null
   */
  public Korisnik dohvati(String korisnik, String lozinka, Boolean prijava) {
    String upit = "SELECT ime, prezime, korisnik, lozinka, email FROM korisnici WHERE korisnik = ?";
    if (prijava) {
      upit += " and lozinka = ?";
    }

    try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
      s.setString(1, korisnik);
      if (prijava) {
        s.setString(2, lozinka);
      }
      ResultSet rs = s.executeQuery();
      if (rs.next()) {
        String ime = rs.getString("ime");
        String prezime = rs.getString("prezime");
        String email = rs.getString("email");
        // Lozinka se ne vraća iz sigurnosnih razloga
        return new Korisnik(korisnik, "******", prezime, ime, email);
      }
    } catch (SQLException ex) {
      Logger.getLogger(KorisnikDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Dohvati sve korisnike iz baze.
   *
   * @return lista svih korisnika ili null u slučaju pogreške
   */
  public List<Korisnik> dohvatiSve() {
    String upit = "SELECT ime, prezime, email, korisnik, lozinka FROM korisnici";
    List<Korisnik> korisnici = new ArrayList<>();

    try (Statement s = this.vezaBP.createStatement();
         ResultSet rs = s.executeQuery(upit)) {
      while (rs.next()) {
        String korisnik1 = rs.getString("korisnik");
        String ime = rs.getString("ime");
        String prezime = rs.getString("prezime");
        String email = rs.getString("email");
        korisnici.add(new Korisnik(korisnik1, "******", prezime, ime, email));
      }
      return korisnici;
    } catch (SQLException ex) {
      Logger.getLogger(KorisnikDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Dodaj novog korisnika u bazu.
   *
   * @param k objekt Korisnik koji se unosi
   * @return true ako je unos uspješan, false inače
   */
  public boolean dodaj(Korisnik k) {
    String upit = "INSERT INTO korisnici (ime, prezime, email, korisnik, lozinka) "
            + "VALUES (?, ?, ?, ?, ?)";

    try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
      s.setString(1, k.ime());
      s.setString(2, k.prezime());
      s.setString(3, k.email());
      s.setString(4, k.korisnik());
      s.setString(5, k.lozinka());
      return s.executeUpdate() == 1;
    } catch (SQLException ex) {
      Logger.getLogger(KorisnikDAO.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }
}

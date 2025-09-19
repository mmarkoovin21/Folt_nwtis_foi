package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3;

import edu.unizg.foi.nwtis.konfiguracije.Konfiguracija;
import edu.unizg.foi.nwtis.konfiguracije.KonfiguracijaApstraktna;
import edu.unizg.foi.nwtis.konfiguracije.NeispravnaKonfiguracija;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KorisnikKupac {
    /**
     * Konfiguracija aplikacije.
     */
  private Konfiguracija konfig;

    /**
     * Skup dozvoljenih komandi.
     */
  private static final Set<String> DOZVOLJENE_KOMANDE = Set.of(
          "JELOVNIK", "KARTAPIĆA", "NARUDŽBA", "JELO", "PIĆE", "RAČUN"
  );

    /**
     * Glavna metoda koja pokreće aplikaciju.
     *
     * @param args Argumenti komandne linije: konfiguracijska datoteka i CSV datoteka s podacima.
     */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Potrebna su dva argumenta: konfiguracijska datoteka i CSV datoteka s podacima.");
      return;
    }

    KorisnikKupac kupac = new KorisnikKupac();
    if (!kupac.ucitajKonfiguraciju(args[0])) {
      System.out.println("Greška pri učitavanju konfiguracije: " + args[0]);
      return;
    }
    kupac.obradiCSV(args[1]);
  }

    /**
     * Učitava konfiguraciju iz datoteke.
     *
     * @param datoteka Putanja do konfiguracijske datoteke.
     * @return true ako je učitavanje uspješno, inače false.
     */
  private boolean ucitajKonfiguraciju(String datoteka) {
    try {
      this.konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(datoteka);
      return true;
    } catch (NeispravnaKonfiguracija ex) {
      return false;
    }
  }

  /**
   * Čita CSV datoteku redak po redak. Svaki redak ima sljedeću strukturu:
   * Korisnik;Adresa;Mrežna vrata;Spavanje;Komanda
   */
  private void obradiCSV(String csvDatoteka) {
    Path putanja = Path.of(csvDatoteka);
    if (!Files.exists(putanja) || !Files.isRegularFile(putanja) || !Files.isReadable(putanja)) {
      return;
    }

    try (BufferedReader citac = Files.newBufferedReader(putanja, StandardCharsets.UTF_8)) {
      String linija;
      while ((linija = citac.readLine()) != null) {
        if (linija.isEmpty()) {
          continue;
        }

        String[] dijelovi = linija.split(";");
        if (dijelovi.length < 5) {
          continue;
        }

        String korisnik = dijelovi[0];
        String adresa = dijelovi[1];
        String mreznaVrataStr = dijelovi[2];
        String spavanjeStr = dijelovi[3];

        StringBuilder komandaBuilder = new StringBuilder(dijelovi[4]);
        for (int i = 5; i < dijelovi.length; i++) {
          komandaBuilder.append(" ").append(dijelovi[i]);
        }
        String komanda = komandaBuilder.toString().trim();

        String[] cmdTokens = komanda.split("\\s+");
        String akcija = cmdTokens[0];

        if (!DOZVOLJENE_KOMANDE.contains(akcija)) {
          continue;
        }

        String korisnikUKomandi = cmdTokens.length > 1 ? cmdTokens[1] : "";
        if (!korisnik.equals(korisnikUKomandi)) {
          continue;
        }

        int port, spavanje;
        try {
          port = Integer.parseInt(mreznaVrataStr);
          spavanje = Integer.parseInt(spavanjeStr);
        } catch (NumberFormatException ex) {
          continue;
        }

        try {
          Thread.sleep(spavanje);
        } catch (InterruptedException e) {
        }

        String odgovor = posaljiZahtjevPosluzitelju(adresa, port, komanda + "\n");
        System.out.println(odgovor);
      }
    } catch (IOException ex) {
    }
  }

  /**
   * Spaja se na poslužitelja na zadanoj adresi i portu, šalje komandu i čita cijeli odgovor.
   */
  public static String posaljiZahtjevPosluzitelju(String adresa, int port, String poruka) {
    try (Socket socket = new Socket(adresa, port)) {
      BufferedReader citac = new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      PrintWriter pisac = new PrintWriter(
              new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

      pisac.write(poruka);
      pisac.flush();
      socket.shutdownOutput();

      StringBuilder graditeljNiza = new StringBuilder();
      String linija;
      while ((linija = citac.readLine()) != null) {
        graditeljNiza.append(linija).append("\n");
      }
      socket.shutdownInput();
      return graditeljNiza.toString();
    } catch (IOException e) {
      return null;
    }
  }
}

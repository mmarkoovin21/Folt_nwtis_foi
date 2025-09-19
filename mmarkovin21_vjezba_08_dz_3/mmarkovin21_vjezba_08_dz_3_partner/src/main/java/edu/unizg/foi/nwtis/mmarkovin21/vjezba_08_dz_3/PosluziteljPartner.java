package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.unizg.foi.nwtis.konfiguracije.Konfiguracija;
import edu.unizg.foi.nwtis.konfiguracije.KonfiguracijaApstraktna;
import edu.unizg.foi.nwtis.konfiguracije.NeispravnaKonfiguracija;
import edu.unizg.foi.nwtis.podaci.Jelovnik;
import edu.unizg.foi.nwtis.podaci.KartaPica;
import edu.unizg.foi.nwtis.podaci.Narudzba;
import edu.unizg.foi.nwtis.podaci.Obracun;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PosluziteljPartner {

    /**
     * Konfiguracijski podaci
     */
    private Konfiguracija konfig;

    private String kodZaKraj = "";

    private final AtomicBoolean kraj = new AtomicBoolean(false);

    /**
     * Skup dretvi
     */
    private static final List<Future<?>> aktivneDretve = new ArrayList<>();

    /**
     * Predložak za kraj i rad s klijentima
     */
    private final Pattern predlozakKraj = Pattern.compile("^KRAJ$");

    /**
     * Predložak za registraciju partnera
     */
    private final Pattern predlozakPartner = Pattern.compile("^PARTNER$");

    /**
     * Predlošci za obradu komandi
     */
    private final Pattern predlozakJelovnik = Pattern.compile(
            "^JELOVNIK\\s+(?<korisnik>\\S+)$");
    /**
     * Predložak za obradu karte pića
     */
    private final Pattern predlozakKartaPica = Pattern.compile(
            "^KARTAPIĆA\\s+(?<korisnik>\\S+)$");
    /**
     * Predložak za obradu jela
     */
    private final Pattern predlozakJelo = Pattern.compile(
            "^JELO\\s+(?<korisnik>\\S+)\\s+(?<idJela>\\S+)\\s+(?<kolicina>\\d+(?:\\.\\d+)?)$");
    /**
     * Predložak za obradu pića
     */
    private final Pattern predlozakPice = Pattern.compile(
            "^PIĆE\\s+(?<korisnik>\\S+)\\s+(?<idPica>\\S+)\\s+(?<kolicina>\\d+(?:\\.\\d+)?)$");
    /**
     * Predložak za obradu narudžbe
     */
    private final Pattern predlozakNarudzba = Pattern.compile(
            "^NARUDŽBA\\s+(?<korisnik>\\S+)$");
    /**
     * Predložak za obradu računa
     */
    private final Pattern predlozakRacun = Pattern.compile(
            "^RAČUN\\s+(?<korisnik>\\S+)$");

    private final Pattern predlozakPauzaKontrola = Pattern.compile("^PAUZA\\s+(?<kod>\\S+)\\s+(?<id>\\d+)$");
    private final Pattern predlozakStartKontrola = Pattern.compile("^START\\s+(?<kod>\\S+)\\s+(?<id>\\d+)$");
    private final Pattern predlozakStatusKontrola = Pattern.compile("^STATUS\\s+(?<kod>\\S+)\\s+(?<id>\\d+)$");
    private final Pattern predlozakSpavanjeKontrola = Pattern.compile("^SPAVA\\s+(?<kod>\\S+)\\s+(?<milisekunde>\\d+)$");
    private static final Pattern predlozakKrajPartnera =
            Pattern.compile("^KRAJ\\s+(?<kod>[A-Za-z0-9_\\-]+)$");
    private final Pattern predlozakStanje = Pattern.compile("^STANJE\\s+(?<korisnik>\\w+)$");
    private final Pattern predlozakOsvjeziKontrola =
            Pattern.compile("^OSVJEŽI\\s+(?<kod>[^\\s]+)$");

    private final AtomicBoolean posluziteljSpava = new AtomicBoolean(false);

    /**
     * Skup svih jelovnika i karte pića
     */
    private final Map<String, Jelovnik> jelovnici = new ConcurrentHashMap<>();
    /**
     * Skup svih karata pića
     */
    private final Map<String, KartaPica> kartaPica = new ConcurrentHashMap<>();
    /**
     * Skup svih narudžbi
     */
    private final Map<String, Queue<Narudzba>> naplaceneNarudzbe = new ConcurrentHashMap<>();
    /**
     * Skup svih plaćenih narudžbi
     */
    private final Queue<Narudzba> placeneNarudzbe = new ConcurrentLinkedQueue<>();
    /**
     * Skup svih zatvorenih veza po dretvima
     */
    private static final Map<Long, AtomicInteger> vezeZatvorenePoDretvi =
            new ConcurrentHashMap<>();

    /**
     * Semafor za dretve.
     */
    private final Lock lockNarudzbe = new ReentrantLock();

    /**
     * Glavna metoda koja pokreće aplikaciju.
     *
     * @param args
     * @throws NeispravnaKonfiguracija
     */
    public static void main(String[] args) throws NeispravnaKonfiguracija {
        if (args.length == 0) {
            System.out.println("Nedovoljan broj argumenata.");
            return;
        }
        if (args.length > 2) {
            System.out.println("Broj argumenata veći od 2.");
            return;
        }

        var program = new PosluziteljPartner();
        var nazivDatoteke = args[0];

        if (!program.ucitajKonfiguraciju(nazivDatoteke)) {
            return;
        }
        if (args.length == 1) {
            program.registrirajPartnera();
        } else {
            var linija = args[1];
            var mKraj = program.predlozakKraj.matcher(linija);
            var mPartner = program.predlozakPartner.matcher(linija);
            if (mKraj.matches()) {
                program.posaljiKraj();
            } else if (mPartner.matches()) {
                program.pokreniRadSKlijentimaiKrajPartnera();
            }
        }
    }

    /**
     * Pokreće rad s klijentima.
     */
    private void pokreniRadSKlijentimaiKrajPartnera() {
        this.kodZaKraj = konfig.dajPostavku("kodZaKraj");
        int pauzaDretve = Integer.parseInt(konfig.dajPostavku("pauzaDretve"));

        if (!ucitajKartuPica() || !ucitajJelovnik()) {
            posaljiKraj();
            return;
        }

        Thread hook = Thread.ofVirtual().unstarted(() -> {
            int prekinutih = 0;
            for (Future<?> f : aktivneDretve) {
                if (!f.isDone()) {
                    f.cancel(true);
                    prekinutih++;
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(hook);

        var factory = Thread.ofVirtual().factory();

        try (var executor = Executors.newThreadPerTaskExecutor(factory)) {
            aktivneDretve.add(executor.submit(this::pokreniPosluziteljaZaKupca));
            aktivneDretve.add(executor.submit(this::pokreniPosluziteljPartnerKraj));

            while (!kraj.get()) {
                try {
                    Thread.sleep(pauzaDretve);
                } catch (InterruptedException e) {
                    break;
                }
            }
            for (Future<?> f : aktivneDretve) {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            }
        }
    }

    /**
     * Pokreće poslužitelja za kupca.
     */
    private void pokreniPosluziteljaZaKupca() {
        int mreznaVrata = Integer.parseInt(konfig.dajPostavku("mreznaVrata"));
        int brojCekaca = Integer.parseInt(konfig.dajPostavku("brojCekaca"));

        var factory = Thread.ofVirtual().factory();

        try (var lokalniExecutor = Executors.newThreadPerTaskExecutor(factory);
             ServerSocket serverSocket = new ServerSocket(mreznaVrata, brojCekaca)) {
            while (!this.kraj.get()) {
                try {
                    Socket klijent = serverSocket.accept();

                    Future<?> f = lokalniExecutor.submit(() -> {
                        try (klijent;
                             BufferedReader citac = new BufferedReader(
                                     new InputStreamReader(klijent.getInputStream(), StandardCharsets.UTF_8));
                             PrintWriter pisac = new PrintWriter(
                                     new OutputStreamWriter(klijent.getOutputStream(), StandardCharsets.UTF_8), true)) {

                            String komanda = citac.readLine();
                            String odgovor = obradiKomanduKupca(komanda);
                            pisac.println(odgovor);
                        } catch (Exception ex) {
                        } finally {
                            evidentirajZatvorenuVezu();
                        }
                    });

                    aktivneDretve.add(f);

                } catch (IOException ex) {
                }
            }
        } catch (IOException ex) {
        }
    }

    /**
     * Pokreće poslužitelja za kraj rada partnera.
     */
    private void pokreniPosluziteljPartnerKraj() {
        int mreznaVrata = Integer.parseInt(konfig.dajPostavku("mreznaVrataKrajPartner"));
        int brojCekaca = Integer.parseInt(konfig.dajPostavku("brojCekaca"));

        var factory = Thread.ofVirtual().factory();

        try (var lokalniExecutor = Executors.newThreadPerTaskExecutor(factory);
             ServerSocket serverSocket = new ServerSocket(mreznaVrata, brojCekaca)) {
            while (!kraj.get()) {
                try {
                    Socket klijent = serverSocket.accept();

                    Future<?> f = lokalniExecutor.submit(() -> {
                        try (klijent;
                             BufferedReader citac = new BufferedReader(
                                     new InputStreamReader(klijent.getInputStream(), StandardCharsets.UTF_8));
                             PrintWriter pisac = new PrintWriter(
                                     new OutputStreamWriter(klijent.getOutputStream(), StandardCharsets.UTF_8), true)) {

                            String komanda = citac.readLine();
                            String odgovor = obradiKomanduKrajRadaPartnera(komanda);
                            pisac.println(odgovor);
                        } catch (Exception ex) {
                        } finally {
                            evidentirajZatvorenuVezu();
                        }
                    });

                    aktivneDretve.add(f);

                } catch (IOException ex) {
                }
            }
        } catch (IOException ex) {
        }
    }

    /**
     * Obrada završne kontrolne komande za partnera.
     *
     * @param komanda Tekstualna komanda primljena od klijenta.
     * @return String odgovor poslužitelja nakon obrade komande.
     * @throws IOException ako dođe do I/O pogreške prilikom parsiranja komande.
     */
    public String obradiKomanduKrajRadaPartnera(String komanda) throws IOException {
        Matcher mKraj    = predlozakKrajPartnera.matcher(komanda);
        Matcher mStart   = predlozakStartKontrola.matcher(komanda);
        Matcher mPauza   = predlozakPauzaKontrola.matcher(komanda);
        Matcher mStatus  = predlozakStatusKontrola.matcher(komanda);
        Matcher mSpavanje= predlozakSpavanjeKontrola.matcher(komanda);
        Matcher mOsvjezi = predlozakOsvjeziKontrola.matcher(komanda);

        if (mStatus.matches()) {
            return obradiStatus(mStatus);
        } else if (mPauza.matches()) {
            return obradiPauza(mPauza);
        } else if (mStart.matches()) {
            return obradiStart(mStart);
        } else if (mSpavanje.matches()) {
            return obradiSpavanje(mSpavanje);
        } else if (mOsvjezi.matches()) {
            return obradiOsvjezi(mOsvjezi);
        } else if (mKraj.matches()) {
            if(obradiKraj(mKraj).contains("OK")) {
                this.kraj.set(true);
            }
            return obradiKraj(mKraj);
        } else {
            return "ERROR 60 - Format komande nije ispravan ili nije ispravan kod za kraj";
        }
    }

    /**
     * Obrada status komande: vraća stanje spavanja/DOSTUPNO.
     *
     * @param mStatus Matcher s grupom "kod" za administratorski kod.
     * @return "OK 1" ili "OK 0" ovisno o stanju ili odgovarajuća ERROR poruka.
     */
    private String obradiStatus(Matcher mStatus) {
        String kodAdmin = this.konfig.dajPostavku("kodZaAdmin");
        String kod      = mStatus.group("kod");
        if (!kod.equals(kodAdmin)) {
            return "ERROR 61 – Pogrešan kodZaAdminPartnera";
        } else if (this.posluziteljSpava.get()) {
            return "ERROR 62 – Pogrešna promjena pauze ili starta";
        } else {
            int stanje = this.posluziteljSpava.get() ? 0 : 1;
            return "OK " + stanje;
        }
    }

    /**
     * Obrada osvježi komande: ponovno učitava jelovnik i kartu pića.
     *
     * @param mOsvjezi Matcher s grupom "kod" za administratorski kod.
     * @return "OK" ako je osvježavanje uspješno ili opis greške.
     */
    private String obradiOsvjezi(Matcher mOsvjezi) {
        String kodAdmin = this.konfig.dajPostavku("kodZaAdmin");
        String kod      = mOsvjezi.group("kod");
        if (!kod.equals(kodAdmin)) {
            return "ERROR 61 – Pogrešan kodZaAdminPartnera";
        } else if (this.posluziteljSpava.get()) {
            return "ERROR 62 – Pogrešna promjena pauze ili starta";
        } else {
            try {
                ucitajKartuPica();
                ucitajJelovnik();
                return "OK";
            } catch (Exception ex) {
                return "Neuspjelo osvježavanje: " + ex.getMessage();
            }
        }
    }

    /**
     * Obrada pauze poslužitelja: stavlja ga u spavanje.
     *
     * @param mPauza Matcher s grupama "kod" i "id" te "milisekunde".
     * @return "OK 0" ako je pauza postavljena ili odgovarajuća ERROR poruka.
     */
    private String obradiPauza(Matcher mPauza) {
        String kodAdmin = this.konfig.dajPostavku("kodZaAdmin");
        String kod = mPauza.group("kod");
        if (!kod.equals(kodAdmin)) {
            return "ERROR 61 – Pogrešan kodZaAdminPartnerastarta";
        } else if (this.posluziteljSpava.get()) {
            return "ERROR 62 – Pogrešna promjena pauze ili starta";
        } else {
            this.posluziteljSpava.set(true);
            return "OK 0\n";
        }
    }

    /**
     * Obrada start komande: budi poslužitelj iz pauze.
     *
     * @param mStart Matcher s grupom "kod".
     * @return "OK 1" ako je poslužitelj uspješno pokrenut ili odgovarajuća ERROR poruka.
     */
    private String obradiStart(Matcher mStart) {
        String kodAdmin = this.konfig.dajPostavku("kodZaAdmin");
        String kod = mStart.group("kod");
        if (!kod.equals(kodAdmin)) {
            return "ERROR 61 – Pogrešan kodZaAdminPartnera";
        } else if (!this.posluziteljSpava.get()) {
            return "ERROR 62 – Pogrešna promjena pauze ili starta";
        } else {
            this.posluziteljSpava.set(false);
            return "OK 1\n";
        }
    }

    /**
     * Obrada spavanje komande: za određeno vrijeme stavlja dretvu u mirovanje.
     *
     * @param mSpavanje Matcher s grupom "kod" i "milisekunde".
     * @return "OK" ako spavanje uspije ili odgovarajuća ERROR poruka.
     */
    private String obradiSpavanje(Matcher mSpavanje) {
        String kodAdmin = this.konfig.dajPostavku("kodZaAdmin");
        String kod = mSpavanje.group("kod");
        int ms = Integer.parseInt(mSpavanje.group("milisekunde"));
        if (!kod.equals(kodAdmin)) {
            return "ERROR 61 – Pogrešan kodZaAdminPartnerastarta";
        } else {
            try {
                Thread.sleep(ms);
                return "OK\n";
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return "ERROR 63 – Prekid spavanja dretve";
            }
        }
    }

    /**
     * Obrada završne komande KRAJ: označava kraj rada partnera.
     *
     * @param mKraj Matcher s grupom "kod".
     * @return "OK" ako je kod valjan ili ERROR poruka ako nije.
     */
    private String obradiKraj(Matcher mKraj) {
        String kod = mKraj.group("kod");
        if (!kod.equals(this.kodZaKraj)) {
            return "ERROR 60 - Format komande nije ispravan ili nije ispravan kod za kraj";
        } else {
            return "OK\n";
        }
    }

    /**
     * Evidentira zatvorenu vezu.
     */
    private static void evidentirajZatvorenuVezu() {
        vezeZatvorenePoDretvi
                .computeIfAbsent(Thread.currentThread().threadId(), id -> new AtomicInteger())
                .incrementAndGet();
    }

    /**
     * Obrada komande kupca.
     *
     * @param komanda komanda
     * @return odgovor
     */
    private String obradiKomanduKupca(String komanda) {
        Matcher podudaranjeJelovnik = predlozakJelovnik.matcher(komanda);
        Matcher podudaranjeKartaPica = predlozakKartaPica.matcher(komanda);
        Matcher podudaranjeJelo = predlozakJelo.matcher(komanda);
        Matcher podudaranjePice = predlozakPice.matcher(komanda);
        Matcher podudaranjeNarudzba = predlozakNarudzba.matcher(komanda);
        Matcher podudaranjeRacun = predlozakRacun.matcher(komanda);
        Matcher podudaranjeStanje = predlozakStanje.matcher(komanda);

        try {
            if (podudaranjeJelovnik.matches()) {
                return obradiJelovnik();
            } else if (podudaranjeKartaPica.matches()) {
                return obradiKartuPica();
            } else if (podudaranjeNarudzba.matches()) {
                return obradiNarudzbu(podudaranjeNarudzba);
            } else if (podudaranjeStanje.matches()) {
                return obradiStanje(podudaranjeStanje);
            } else if (podudaranjeJelo.matches()) {
                return obradiJelo(podudaranjeJelo);
            } else if (podudaranjePice.matches()) {
                return obradiPice(podudaranjePice);
            } else if (podudaranjeRacun.matches()) {
                return obradiRacun(podudaranjeRacun);
            } else {
                return "ERROR 40 - Format komande nije ispravan";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR 49 - Nešto drugo nije u redu";
        }
    }

    /**
     * Obrada stanja narudžbi za korisnika.
     *
     * @param podudaranjeNarudzba podudaranje narudžbe
     * @return odgovor
     */
    private String obradiStanje(Matcher podudaranjeNarudzba) {
        String korisnik = podudaranjeNarudzba.group("korisnik");
        if (this.posluziteljSpava.get()) {
            return "ERROR 48 - Poslužitelj za prijem zahtjeva kupaca u pauzi";
        }
        Queue<Narudzba> queue = naplaceneNarudzbe.getOrDefault(korisnik, new LinkedList<>());
        List<Narudzba> narudzbeZaKupca = new ArrayList<>(queue);
        Gson gson = new Gson();
        String jsonLista = gson.toJson(narudzbeZaKupca);
        return "OK\n" + jsonLista;
    }

    /**
     * Obrada računa.
     *
     * @param podudaranjeRacun podudaranje računa
     * @return odgovor
     */
    private String obradiRacun(Matcher podudaranjeRacun) {
        int kvotaNarudzbi = Integer.parseInt(this.konfig.dajPostavku("kvotaNarudzbi"));
        String partnerSigKod = this.konfig.dajPostavku("sigKod");
        String adresaTvrtka = this.konfig.dajPostavku("adresa");
        int mreznaVrataTvrtka = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataRad"));
        String korisnik = podudaranjeRacun.group("korisnik");

        lockNarudzbe.lock();
        try {
            if (!naplaceneNarudzbe.containsKey(korisnik) || naplaceneNarudzbe.get(korisnik).isEmpty()) {
                return "ERROR 43 - Ne postoji otvorena narudžba za korisnika/kupca.";
            }

            Queue<Narudzba> otvoreneNarudzbe = naplaceneNarudzbe.get(korisnik);

            if (otvoreneNarudzbe.size() % kvotaNarudzbi == 0) {
                Map<String, Narudzba> agregirani = new HashMap<>();
                for (Narudzba n : otvoreneNarudzbe) {
                    String proizvodId = n.id();
                    agregirani.merge(proizvodId, n, (postojeci, novi) -> new Narudzba(
                            postojeci.korisnik(),
                            proizvodId,
                            postojeci.jelo(),
                            postojeci.kolicina() + novi.kolicina(),
                            postojeci.cijena(),
                            System.currentTimeMillis()
                    ));
                }

                List<Obracun> listaObracuna = new ArrayList<>();
                int partnerId = Integer.parseInt(this.konfig.dajPostavku("id"));
                long vrijeme = System.currentTimeMillis();
                for (Narudzba n : agregirani.values()) {
                    listaObracuna.add(new Obracun(
                            partnerId,
                            n.id(),
                            n.jelo(),
                            n.kolicina(),
                            n.cijena(),
                            vrijeme
                    ));
                }

                Gson gson = new Gson();
                String jsonObracun = gson.toJson(listaObracuna);
                String poruka = "OBRAČUN " + partnerId + " " + partnerSigKod + "\n" + jsonObracun + "\n";
                String odgovor = posaljiZahtjevPosluzitelju(adresaTvrtka, mreznaVrataTvrtka, poruka);

                if (odgovor != null && odgovor.startsWith("OK")) {
                    placeneNarudzbe.addAll(otvoreneNarudzbe);
                    naplaceneNarudzbe.remove(korisnik);
                    return "OK\n";
                } else {
                    return "ERROR 45 - Neuspješno slanje obračuna";
                }
            } else {
                return "OK\n";
            }
        } finally {
            lockNarudzbe.unlock();
        }
    }

    /**
     * Obrada jelovnika.
     *
     * @return odgovor
     */
    private String obradiNarudzbu(Matcher podudaranjeNarudzba) {
        String korisnik = podudaranjeNarudzba.group("korisnik");

        lockNarudzbe.lock();
        try {
            Narudzba novaNarudzba = new Narudzba(
                    korisnik,
                    "",
                    false,
                    0.0f,
                    0.0f,
                    System.currentTimeMillis()
            );
            naplaceneNarudzbe
                    .computeIfAbsent(korisnik, k -> new ConcurrentLinkedQueue<>())
                    .add(novaNarudzba);
            return "OK\n";
        } finally {
            lockNarudzbe.unlock();
        }
    }


    /**
     * Obrada jelovnika.
     *
     * @return odgovor
     */
    private String obradiPice(Matcher podudaranjePice) {
        String korisnik = podudaranjePice.group("korisnik");
        String idPica = podudaranjePice.group("idPica");
        String kolicina = podudaranjePice.group("kolicina");

        lockNarudzbe.lock();
        try {
            if (!kartaPica.containsKey(idPica)) {
                return "ERROR 42 - Ne postoji piće s id u kolekciji karte pića kod partnera";
            } else if (!naplaceneNarudzbe.containsKey(korisnik)) {
                return "ERROR 43 - Ne postoji otvorena narudžba za korisnika/kupca.";
            } else {
                float cijena = this.kartaPica.get(idPica).cijena();
                Narudzba novaNarudzba = new Narudzba(
                        korisnik,
                        idPica,
                        false,
                        Float.parseFloat(kolicina),
                        cijena,
                        System.currentTimeMillis()
                );
                naplaceneNarudzbe.get(korisnik).add(novaNarudzba);
                return "OK\n";
            }
        } finally {
            lockNarudzbe.unlock();
        }
    }

    /**
     * Obrada jelovnika.
     *
     * @param podudaranjeJelo podudaranje jela
     * @return odgovor
     */
    private String obradiJelo(Matcher podudaranjeJelo) {
        String korisnik = podudaranjeJelo.group("korisnik");
        String idJela = podudaranjeJelo.group("idJela");
        String kolicina = podudaranjeJelo.group("kolicina");

        lockNarudzbe.lock();
        try {
            if (!jelovnici.containsKey(idJela)) {
                return "ERROR 41 - Ne postoji jelo s id u kolekciji jelovnika kod partnera";
            } else if (!naplaceneNarudzbe.containsKey(korisnik) || naplaceneNarudzbe.get(korisnik).isEmpty()) {
                return "ERROR 43 - Ne postoji otvorena narudžba za korisnika/kupca.";
            } else {
                float cijena = this.jelovnici.get(idJela).cijena();
                Narudzba novaNarudzba = new Narudzba(
                        korisnik,
                        idJela,
                        false,
                        Float.parseFloat(kolicina),
                        cijena,
                        System.currentTimeMillis()
                );
                naplaceneNarudzbe.get(korisnik).add(novaNarudzba);
                return "OK\n";
            }
        } finally {
            lockNarudzbe.unlock();
        }
    }

    /**
     * Obrada jelovnika.
     *
     * @return odgovor
     */
    private String obradiKartuPica() {
        if (this.kartaPica.isEmpty()) {
            return "ERROR 47 - Neuspješno preuzimanje karte pića";
        }
        Gson gson = new Gson();
        String json = gson.toJson(this.kartaPica.values());

        return "OK\n" + json;
    }

    /**
     * Obrada jelovnika.
     *
     * @return odgovor
     */
    private String obradiJelovnik() {
        if (this.jelovnici.isEmpty()) {
            return "ERROR 46 - Neuspješno preuzimanje jelovnika";
        }
        Gson gson = new Gson();
        String json = gson.toJson(this.jelovnici.values());

        return "OK\n" + json;
    }

    /**
     * Učitava kartu pića.
     *
     * @return true, ako je uspješno učitana karta pića
     */
    private Boolean ucitajKartuPica() {
        int idPartnera = Integer.parseInt(this.konfig.dajPostavku("id"));
        String sigurnosniKod = this.konfig.dajPostavku("sigKod");
        String adresa = this.konfig.dajPostavku("adresa");
        int mreznaVrata = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataRad"));
        String poruka = "KARTAPIĆA " + idPartnera + " " + sigurnosniKod + "\n";
        String odgovor = posaljiZahtjevPosluzitelju(adresa, mreznaVrata, poruka);
        if (odgovor == null) {
            return false;
        }
        try {
            String jsonKartaPica = odgovor.substring(3).trim();
            Gson gson = new Gson();
            KartaPica[] kartaPicaArray = gson.fromJson(jsonKartaPica, KartaPica[].class);
            Arrays.stream(kartaPicaArray).forEach(ob -> this.kartaPica.put(ob.id(), ob));
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Učitava jelovnik.
     *
     * @return true, ako je uspješno učitan jelovnik
     */
    public Boolean ucitajJelovnik() {
        int idPartnera = Integer.parseInt(this.konfig.dajPostavku("id"));
        String sigurnosniKod = this.konfig.dajPostavku("sigKod");
        String adresa = this.konfig.dajPostavku("adresa");
        int mreznaVrata = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataRad"));
        String poruka = "JELOVNIK " + idPartnera + " " + sigurnosniKod + "\n";
        String odgovor = posaljiZahtjevPosluzitelju(adresa, mreznaVrata, poruka);
        if (odgovor == null || !odgovor.startsWith("OK")) {
            return false;
        }
        try {
            String jsonJelovnik = odgovor.substring(3).trim();
            Gson gson = new Gson();
            Jelovnik[] jelovnikLista = gson.fromJson(jsonJelovnik, Jelovnik[].class);
            Arrays.stream(jelovnikLista).forEach(ob -> this.jelovnici.put(ob.id(), ob));
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Šalje zahtjev za kraj poslužitelju.
     */
    public void posaljiKraj() {
        String kodZaKraj = this.konfig.dajPostavku("kodZaKraj");
        String adresa = this.konfig.dajPostavku("adresa");
        int mreznaVrata = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataKraj"));

        String poruka = "KRAJ " + kodZaKraj + "\n";

        String odgovor = posaljiZahtjevPosluzitelju(adresa, mreznaVrata, poruka);
        if ("OK".equals(odgovor)) {
            System.out.println("Uspješan kraj poslužitelja.");
        } else {
            System.out.println("Neuspješan kraj poslužitelja. Odgovor: " + odgovor);
        }
    }

    /**
     * Registrira partnera.
     *
     * @throws NeispravnaKonfiguracija
     */
    public void registrirajPartnera() throws NeispravnaKonfiguracija {
        String nazivPartnera = this.konfig.dajPostavku("naziv");
        int idPartnera = Integer.parseInt(this.konfig.dajPostavku("id"));
        String vrstaKuhinje = this.konfig.dajPostavku("kuhinja");
        String adresa = this.konfig.dajPostavku("adresa");
        int mreznaVrata = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataRegistracija"));
        double gpsSirina = Double.parseDouble(this.konfig.dajPostavku("gpsSirina"));
        double gpsDuzina = Double.parseDouble(this.konfig.dajPostavku("gpsDuzina"));
        Pattern predlozakOdgovor = Pattern.compile("^OK (?<sigurnosniKod>[a-fA-F0-9]+)$");

        String poruka = "PARTNER " + idPartnera + " \"" + nazivPartnera + "\" " + vrstaKuhinje + " " + adresa + " " + mreznaVrata + " " + gpsSirina + " " + gpsDuzina + "\n";

        String odgovor = posaljiZahtjevPosluzitelju(adresa, mreznaVrata, poruka);
        Matcher podudaranjeOdgocor = predlozakOdgovor.matcher(odgovor);
        if (odgovor == null || !podudaranjeOdgocor.matches()) {
        } else {
            String sigurnosniKod = podudaranjeOdgocor.group("sigurnosniKod");
            if (this.konfig.dajPostavku("sigKod") == null) {
                this.konfig.spremiPostavku("sigKod", sigurnosniKod);
            } else {
                this.konfig.azurirajPostavku("sigKod", sigurnosniKod);
            }
            this.konfig.spremiKonfiguraciju();
        }
    }

    /**
     * Šalje zahtjev poslužitelju.
     *
     * @param adresa      adresa poslužitelja
     * @param mreznaVrata mrežna vrata
     * @param poruka      poruka
     * @return odgovor
     */
    public static String posaljiZahtjevPosluzitelju(String adresa, int mreznaVrata, String poruka) {
        try (Socket mreznaUticnica = new Socket(adresa, mreznaVrata)) {
            BufferedReader citac = new BufferedReader(
                    new InputStreamReader(mreznaUticnica.getInputStream(), StandardCharsets.UTF_8));
            OutputStream izlaz = mreznaUticnica.getOutputStream();
            PrintWriter pisac = new PrintWriter(new OutputStreamWriter(izlaz, StandardCharsets.UTF_8), true);
            pisac.write(poruka);
            pisac.flush();
            mreznaUticnica.shutdownOutput();
            StringBuilder graditeljNiza = new StringBuilder();
            String line;
            while ((line = citac.readLine()) != null) {
                graditeljNiza.append(line).append("\n");
            }
            mreznaUticnica.shutdownInput();
            mreznaUticnica.close();
            return graditeljNiza.toString().trim();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Ucitaj konfiguraciju.
     *
     * @param nazivDatoteke naziv datoteke
     * @return true, ako je uspješno učitavanje konfiguracije
     */
    private boolean ucitajKonfiguraciju(String nazivDatoteke) {
        try {
            this.konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
            return true;
        } catch (NeispravnaKonfiguracija ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
}
    package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3;

    import com.google.gson.*;
    import edu.unizg.foi.nwtis.konfiguracije.Konfiguracija;
    import edu.unizg.foi.nwtis.konfiguracije.KonfiguracijaApstraktna;
    import edu.unizg.foi.nwtis.konfiguracije.NeispravnaKonfiguracija;
    import edu.unizg.foi.nwtis.podaci.*;

    import java.io.*;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.net.URI;
    import java.net.http.HttpClient;
    import java.net.http.HttpRequest;
    import java.net.http.HttpResponse;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.StandardOpenOption;
    import java.time.Duration;
    import java.util.*;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.concurrent.CopyOnWriteArrayList;
    import java.util.concurrent.Executors;
    import java.util.concurrent.Future;
    import java.util.concurrent.atomic.AtomicBoolean;
    import java.util.concurrent.atomic.AtomicInteger;
    import java.util.concurrent.locks.Lock;
    import java.util.concurrent.locks.ReentrantLock;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    /**
     * Poslužitelj za upravljanje partnerima i njihovim jelovnicima i kartama pića.
     * Ovaj poslužitelj omogućuje registraciju partnera, obradu njihovih jelovnika,
     * karata pića i obračuna, te upravljanje vezama i kontrolom rada.
     */
    public class PosluziteljTvrtka {

        /**
         * Konfiguracija aplikacije.
         */
        protected Konfiguracija konfig;

        /**
         * Skup dozvoljenih komandi.
         */
        private String kodZaKraj = "";

        /**
         * Zastavica za kraj rada.
         */
        private final AtomicBoolean kraj = new AtomicBoolean(false);

        private final Map<Integer, AtomicBoolean> idPosluzitelja = new ConcurrentHashMap<>();


        /**
         * Predlozak za registraciju.
         */
        protected final Pattern predlozakRegistracija = Pattern.compile(
                "^PARTNER (?<id>\\d+)\\s+\"(?<naziv>[^\"]+)\"\\s+(?<vrstaKuhinje>\\S+)\\s+(?<adresa>\\S+)\\s+(?<mreznaVrata>\\d+)\\s+(?<gpsSirina>\\d+\\.\\d+)\\s+(?<gpsDuzina>\\d+\\.\\d+)\\s+(?<adminKod>\\d+\\.\\d+)$");

        /**
         * Predložak za obradu brisanja partnera.
         */
        protected final Pattern predlozakBrisanje = Pattern.compile(
                "^OBRIŠI (?<id>\\d+) (?<sigurnosniKod>[a-fA-F0-9]+)$");

        /**
         * Predložak za popis partnera.
         */
        private final Pattern predlozakPopis = Pattern.compile("^POPIS$");

        /**
         * Predložak za obradu kraja.
         */
        private final Pattern predlozakKraj = Pattern.compile("^KRAJ (?<kod>[A-Z0-9]+)$");
        private final Pattern predlozakKrajWs = Pattern.compile("^KRAJWS (?<kod>[A-Z0-9]+)$");
        /**
         * Predložak za obradu jelovnika.
         */
        protected final Pattern predlozakJelovnik = Pattern.compile(
                "^JELOVNIK\\s+(?<id>\\d+)\\s+(?<sigurnosniKod>[a-fA-F0-9]+)$");
        /**
         * Predložak za obradu karte pića.
         */
        protected final Pattern predlozakKartaPica = Pattern.compile(
                "^KARTAPIĆA\\s+(?<id>\\d+)\\s+(?<sigurnosniKod>[a-fA-F0-9]+)$");
        /**
         * Predložak za obradu obračunaWS.
         */
        private final Pattern predlozakObracunWs = Pattern.compile("^OBRAČUNWS\\s+(?<id>\\d+)\\s+(?<sigurnosniKod>[a-fA-F0-9]+)$");

        /**
         * Predložak za obradu obračuna.
         */
        private final Pattern predlozakObracun = Pattern.compile("^OBRAČUN\\s+(?<id>\\d+)\\s+(?<sigurnosniKod>[a-fA-F0-9]+)$");

        /**
         * Predložak za obradu pauze.
         */
        private final Pattern predlozakPauzaKontrola = Pattern.compile("^PAUZA\\s+(?<kod>\\S+)\\s+(?<id>\\d+)$");

        /**
         * Predložak za obradu statusa.
         */
        private final Pattern predlozakStartKontrola = Pattern.compile("^START\\s+(?<kod>\\S+)\\s+(?<id>\\d+)$");

        /**
         * Predložak za obradu spavanja.
         */
        private final Pattern predlozakStatusKontrola = Pattern.compile("^STATUS\\s+(?<kod>\\S+)\\s+(?<id>\\d+)$");

        /**
         * Predložak za obradu spavanja.
         */
        private final Pattern predlozakSpavanjeKontrola = Pattern.compile("^SPAVA\\s+(?<kod>\\S+)\\s+(?<milisekunde>\\d+)$");

        /**
         * Predložak za osvježavanje kontrole.
         */
        private final Pattern predlozakOsvjeziKontrola =
                Pattern.compile("^OSVJEŽI\\s+(?<kod>[^\\s]+)$");
        /**
         * Skup aktivnih dretvi.
         */
        private static final List<Future<?>> aktivneDretve = new CopyOnWriteArrayList<>();
        /**
         * Mapa za pohranu zatvorenih veza po dretvima.
         */
        private static final Map<Long, AtomicInteger> vezeZatvorenePoDretvi = new ConcurrentHashMap<>();

        /**
         * Mape za pohranu kuhinja.
         */
        private final Map<String, String> kuhinje = new ConcurrentHashMap<>();
        /**
         * Mapa za pohranu jelovnika.
         */
        protected final Map<String, List<Jelovnik>> jelovnici = new ConcurrentHashMap<>();
        /**
         * Mapa za pohranu karte pića.
         */
        protected final Map<String, KartaPica> kartaPica = new ConcurrentHashMap<>();
        /**
         * Mapa za pohranu partnera.
         */
        protected final Map<Integer, Partner> partneri = new ConcurrentHashMap<>();
        /**
         * Mapa za pohranu obračuna.
         */
        protected final Map<String, Obracun> obracuni = new ConcurrentHashMap<>();

        /**
         * Semafor za dretve.
         */
        private final Lock lockObracun = new ReentrantLock();

        /**
         * Glavna metoda koja pokreće aplikaciju.
         *
         * @param args Argumenti komandne linije: konfiguracijska datoteka.
         */
        public static void main(String[] args) {
            if (args.length != 1) {
                System.out.println("Broj argumenata nije 1.");
                return;
            }

            var program = new PosluziteljTvrtka();
            var nazivDatoteke = args[0];

            program.pripremiKreni(nazivDatoteke);
        }

        /**
         * Priprema i pokreće aplikaciju.
         *
         * @param nazivDatoteke naziv datoteke konfiguracije
         */
        public void pripremiKreni(String nazivDatoteke) {
            if (!ucitajKonfiguraciju(nazivDatoteke)
                    || !ucitajPartnereIzDatoteke()
                    || !ucitajKartuPica()
                    || !ucitajJelovnike()) {
                return;
            }
            this.kodZaKraj = konfig.dajPostavku("kodZaKraj");
            int pauzaDretveZaKraj = Integer.parseInt(konfig.dajPostavku("pauzaDretve"));

            var factory = Thread.ofVirtual().factory();

            try (var executor = Executors.newThreadPerTaskExecutor(factory)) {

                Thread shutdownHook = Thread.ofVirtual().unstarted(() -> {
                    kraj.set(true);

                    int ukupnoPrekinutih = 0;
                    for (Future<?> f : aktivneDretve) {
                        if (!f.isDone()) {
                            f.cancel(true);
                            ukupnoPrekinutih++;
                        }
                    }
                });

                Runtime.getRuntime().addShutdownHook(shutdownHook);

                aktivneDretve.add(executor.submit(this::pokreniPosluziteljKraj));
                aktivneDretve.add(executor.submit(this::pokreniPosluziteljaZaRegistracijuPartnera));
                aktivneDretve.add(executor.submit(this::pokreniPosluziteljPartneri));

                idPosluzitelja.put(1, new AtomicBoolean(true));
                idPosluzitelja.put(2, new AtomicBoolean(true));

                while (!kraj.get()) {
                    try {
                        Thread.sleep(pauzaDretveZaKraj);
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
         * Evidentira zatvorenu vezu.
         */
        private static void evidentirajZatvorenuVezu() {
            vezeZatvorenePoDretvi
                    .computeIfAbsent(Thread.currentThread().threadId(), tr -> new AtomicInteger())
                    .incrementAndGet();
        }

        /**
         * Učitava jelovnike iz konfiguracijske datoteke.
         *
         * @return true ako su svi jelovnici učitani, inače false
         */
        protected boolean ucitajJelovnike() {
            Properties svePostavke = konfig.dajSvePostavke();

            for (String kljuc : svePostavke.stringPropertyNames()) {
                if (kljuc.matches("kuhinja_\\d+")) {
                    String vrijednost = konfig.dajPostavku(kljuc);
                    if (vrijednost == null) {
                        continue;
                    }
                    String[] dijelovi = vrijednost.split(";");
                    if (dijelovi.length < 2) {
                        continue;
                    }
                    String oznaka = dijelovi[0].trim();

                    String nazivKuhinje = dijelovi[1].trim();
                    kuhinje.put(oznaka, nazivKuhinje);

                    Path putanja = Path.of(kljuc + ".json");
                    if (Files.exists(putanja) && Files.isRegularFile(putanja) && Files.isReadable(putanja)) {
                        try (BufferedReader citac = Files.newBufferedReader(putanja)) {
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            var jsonArray = JsonParser.parseReader(citac).getAsJsonArray();
                            List<Jelovnik> listaJelovnika = new ArrayList<>();
                            for (JsonElement element : jsonArray) {
                                Jelovnik jelovnik = gson.fromJson(element, Jelovnik.class);
                                listaJelovnika.add(jelovnik);
                            }
                            if (!listaJelovnika.isEmpty()) {
                                jelovnici.put(oznaka, listaJelovnika);
                            } else {
                            }
                        } catch (IOException ex) {
                        }
                    }
                }
            }
            return true;
        }

        /**
         * Pokreće poslužitelja za registraciju partnera.
         */
        public void pokreniPosluziteljaZaRegistracijuPartnera() {
            int mreznaVrataRegistracija = Integer.parseInt(
                    konfig.dajPostavku("mreznaVrataRegistracija"));
            int brojCekaca = 0;

            var tvornica = Thread.ofVirtual().factory();

            try (var lokalniExecutor = Executors.newThreadPerTaskExecutor(tvornica);
                 ServerSocket serverSocket = new ServerSocket(mreznaVrataRegistracija, brojCekaca)) {
                while (!kraj.get() && idPosluzitelja.get(1).get()) {
                    try {
                        Socket klijent = serverSocket.accept();

                        Future<?> f = lokalniExecutor.submit(() -> {
                            try (klijent;
                                 BufferedReader citac = new BufferedReader(
                                         new InputStreamReader(klijent.getInputStream(), StandardCharsets.UTF_8));
                                 PrintWriter pisac = new PrintWriter(
                                         new OutputStreamWriter(klijent.getOutputStream(), StandardCharsets.UTF_8), true)) {

                                String komanda = citac.readLine();
                                String odgovor = obradiKomanduPosluziteljZaRegistracijuPartnera(komanda);
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
         * Pokreće poslužitelja za partnere.
         */
        public void pokreniPosluziteljPartneri() {
            int mreznaVrataPartneri = Integer.parseInt(
                    konfig.dajPostavku("mreznaVrataRad"));
            int brojCekaca = 0;

            var tvornica = Thread.ofVirtual().factory();

            try (var lokalniExecutor = Executors.newThreadPerTaskExecutor(tvornica);
                 ServerSocket serverSocket = new ServerSocket(mreznaVrataPartneri, brojCekaca)) {
                while (!kraj.get() && idPosluzitelja.get(2).get()) {
                    try {
                        Socket klijent = serverSocket.accept();

                        Future<?> f = lokalniExecutor.submit(() -> {
                            try (klijent;
                                 BufferedReader citac = new BufferedReader(
                                         new InputStreamReader(klijent.getInputStream(), StandardCharsets.UTF_8));
                                 PrintWriter pisac = new PrintWriter(
                                         new OutputStreamWriter(klijent.getOutputStream(), StandardCharsets.UTF_8), true)) {

                                String komanda = citac.readLine();
                                String odgovor = obradiKomanduPartneri(komanda, citac);
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
         * Obrada JSON obračuna.
         *
         * @param in ulazni tok
         * @return niz obračuna
         */
        private Obracun[] obradiJsonObracun(BufferedReader in) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            try {
                while (in.ready() && (line = in.readLine()) != null) {
                    jsonBuilder.append(line);
                    if (line.contains("]")) {
                        break;
                    }
                }
                String jsonString = jsonBuilder.toString();

                JsonElement jsonElement = JsonParser.parseString(jsonString);
                if (!jsonElement.isJsonArray()) {
                    return null;
                }

                Gson gson = new Gson();
                return gson.fromJson(jsonString, Obracun[].class);
            } catch (JsonSyntaxException | IOException e) {
                return null;
            }
        }

        /**
         * Obrada komande za registraciju partnera.
         *
         * @param komanda ulazna komanda
         * @return odgovor
         */
        public String obradiKomanduPosluziteljZaRegistracijuPartnera(String komanda) {
            Matcher mRegistracija = predlozakRegistracija.matcher(komanda);
            Matcher mBrisanje = predlozakBrisanje.matcher(komanda);
            Matcher mPopis = predlozakPopis.matcher(komanda);

            try {
                if (mRegistracija.matches()) {
                    return obradiRegistraciju(mRegistracija);
                } else if (mBrisanje.matches()) {
                    return obradiBrisanje(mBrisanje);
                } else if (mPopis.matches()) {
                    return obradiPopis();
                } else {
                    return "ERROR 20 - Format komande nije ispravan";
                }
            } catch (Exception e) {
                return "ERROR 29 - Greška prilikom obrade komande";
            }
        }

        /**
         * Obrada brisanja partnera.
         *
         * @param m ulazni matcher
         * @return odgovor
         */
        public String obradiBrisanje(Matcher m) {
            int id = Integer.parseInt(m.group("id"));
            String sigurnosniKod = m.group("sigurnosniKod");

            Partner partner = partneri.get(id);
            if (partner == null) {
                return "ERROR 23 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera";
            }

            if (!partner.sigurnosniKod().equals(sigurnosniKod)) {
                return "ERROR 22 - Neispravan sigurnosni kod partnera";
            }

            partneri.remove(id);
            azurirajDatotekuPartnera();

            return "OK";
        }

        /**
         * Obrada komande za partnere.
         *
         * @param komanda ulazna komanda
         * @param in     ulazni tok
         * @return odgovor
         */
        private String obradiKomanduPartneri(String komanda, BufferedReader in) {
            Matcher mJelovnik = predlozakJelovnik.matcher(komanda);
            Matcher mKartaPica = predlozakKartaPica.matcher(komanda);
            Matcher mObracun = predlozakObracun.matcher(komanda);
            Matcher mObracunWs = predlozakObracunWs.matcher(komanda);
            if (mJelovnik.matches()) {
                return obradiJelovnik(mJelovnik);
            } else if (mKartaPica.matches()) {
                return obradiKartaPica(mKartaPica);
            } else if (mObracun.matches()) {
                return obradiObracun(mObracun, in);
            } else if (mObracunWs.matches()) {
                return obradiObracunWs(mObracunWs, in);
            }
            return "ERROR 30 - Format komande nije ispravan";
        }

        /**
         * Obrada obračuna.
         *
         * @param m  ulazni matcher
         * @param in ulazni tok
         * @return odgovor
         */
        public String obradiObracunWs(Matcher m, BufferedReader in) {
            int id = Integer.parseInt(m.group("id"));
            String predaniSigurnosniKod = m.group("sigurnosniKod");
            String nazivDatotekeObracuna = konfig.dajPostavku("datotekaObracuna");
            Path obracunFile = Path.of(nazivDatotekeObracuna);

            Partner partner = partneri.get(id);
            if (partner == null || !partner.sigurnosniKod().equals(predaniSigurnosniKod)) {
                return "ERROR 31 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera";
            }

            lockObracun.lock();
            try {
                Obracun[] noviObracun = obradiJsonObracun(in);
                ucitajObracuneIzDatoteke(obracunFile);

                if (noviObracun != null) {
                    boolean allMatch = Arrays.stream(noviObracun).allMatch(ob -> ob.partner() == id);
                    if (!allMatch) {
                        return "ERROR 35 - Neispravan obračun";
                    }
                    for (Obracun ob : noviObracun) {
                        this.obracuni.put(ob.id(), ob);
                    }
                } else {
                    return "ERROR 35 - Neispravan obračun";
                }

                return spremObracuneUDatoteku(obracunFile);
            } finally {
                lockObracun.unlock();
            }
        }

        /**
         * Obrada registracije partnera.
         *
         * @param m ulazni matcher
         * @return odgovor
         */
        public String obradiObracun(Matcher m, BufferedReader in) {
            int id = Integer.parseInt(m.group("id"));
            String predaniSigurnosniKod = m.group("sigurnosniKod");
            String nazivDatotekeObracuna = konfig.dajPostavku("datotekaObracuna");
            Path obracunFile = Path.of(nazivDatotekeObracuna);

            Partner partner = partneri.get(id);
            if (partner == null || !partner.sigurnosniKod().equals(predaniSigurnosniKod)) {
                return "ERROR 31 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera";
            }
            lockObracun.lock();
            try {
                Obracun[] noviObracun = obradiJsonObracun(in);
                ucitajObracuneIzDatoteke(obracunFile);
                if (noviObracun != null) {
                    boolean allMatch = Arrays.stream(noviObracun).allMatch(ob -> ob.partner() == id);
                    if (!allMatch) {
                        return "ERROR 35 - Neispravan obračun";
                    }
                    for (Obracun ob : noviObracun) {
                        this.obracuni.put(ob.id(), ob);
                    }
                } else {
                    return "ERROR 35 - Neispravan obračun";
                }

                String spremljeno =  spremObracuneUDatoteku(obracunFile);
                if (!"OK".contains(spremljeno)) {
                    return spremljeno;
                }
                String restAdresa = konfig.dajPostavku("restAdresa");
                URI endpoint = URI.create(restAdresa);
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .create();
                String jsonBody = gson.toJson(noviObracun);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(endpoint)
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    return "OK";
                } else {
                    return "ERROR 36 – Poslužitelj za partnere u pauzi ";
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return "ERROR 37 – RESTful zahtjev nije uspješan" + e.getMessage();
            } finally {
                lockObracun.unlock();
            }
        }

        /**
         * Učitava obračune iz datoteke.
         *
         * @param obracunFile putanja do datoteke
         */
        private void ucitajObracuneIzDatoteke(Path obracunFile) {
            try {
                if (Files.notExists(obracunFile)) {
                    Files.writeString(
                            obracunFile,
                            "[]",
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE
                    );
                }

                if (Files.isRegularFile(obracunFile) && Files.isReadable(obracunFile)) {
                    try (BufferedReader citac = Files.newBufferedReader(obracunFile)) {
                        Gson gson = new Gson();
                        JsonElement jsonElement = JsonParser.parseReader(citac);

                        Obracun[] postojeciObracuni;
                        if (jsonElement.isJsonArray()) {
                            postojeciObracuni = gson.fromJson(jsonElement, Obracun[].class);
                            Arrays.stream(postojeciObracuni)
                                    .forEach(ob -> this.obracuni.put(ob.id(), ob));
                        }
                    } catch (JsonSyntaxException ex) {
                    }
                }
            } catch (IOException ex) {
            }
        }


        /**
         * Spremanje obračuna u datoteku.
         *
         * @param obracunFile putanja do datoteke
         * @return odgovor
         */
        private String spremObracuneUDatoteku(Path obracunFile) {
            try (BufferedWriter pisac = Files.newBufferedWriter(obracunFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(this.obracuni.values(), pisac);
                return "OK\n";
            } catch (IOException e) {
                return "ERROR 35 - Neispravan obračun";
            }
        }

        /**
         * Obrada karte pića.
         *
         * @param m ulazni matcher
         * @return odgovor
         */
        public String obradiKartaPica(Matcher m) {
            try {
                int id = Integer.parseInt(m.group("id"));
                String predaniSigurnosniKod = m.group("sigurnosniKod");

                Partner partner = partneri.get(id);
                if (partner == null || !partner.sigurnosniKod().equals(predaniSigurnosniKod)) {
                    return "ERROR 31 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera";
                }

                if (kartaPica.isEmpty()) {
                    return "ERROR 34 - Neispravna karta pića";
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonKartaPica = gson.toJson(kartaPica.values());
                return "OK\n" + jsonKartaPica;
            } catch (Exception ex) {
                return "ERROR 39 - Nešto drugo nije u redu.";
            }
        }

        /**
         * Obrada jelovnika.
         *
         * @param m ulazni matcher
         * @return odgovor
         */
        public String obradiJelovnik(Matcher m) {
            try {
                int id = Integer.parseInt(m.group("id"));
                String predaniSigurnosniKod = m.group("sigurnosniKod");

                Partner partner = partneri.get(id);
                if (partner == null || !partner.sigurnosniKod().equals(predaniSigurnosniKod)) {
                    return "ERROR 23 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera";
                }

                String oznakaKuhinje = partner.vrstaKuhinje();
                List<Jelovnik> listaJelovnika = jelovnici.get(oznakaKuhinje);
                if (listaJelovnika == null) {
                    listaJelovnika = new ArrayList<>();
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonJelovnika = gson.toJson(listaJelovnika);

                return "OK\n" + jsonJelovnika;
            } catch (Exception ex) {
                return "ERROR 29 - Nešto drugo nije u redu.";
            }
        }

        /**
         * Pokreće poslužitelj za kraj.
         */
        public void pokreniPosluziteljKraj() {
            var mreznaVrataKraj = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataKraj"));
            var brojCekaca = 0;
            try (ServerSocket ss = new ServerSocket(mreznaVrataKraj, brojCekaca)) {
                while (!this.kraj.get()) {
                    var mreznaUticnica = ss.accept();
                    this.obradiKomanduKraj(mreznaUticnica);
                }
            } catch (IOException ex) {
                System.out.println("Greška prilikom pokretanja poslužitelja za kraj: " + ex.getMessage());
            }
        }

        /**
         * Učitava kartu pića iz datoteke.
         *
         * @return true ako je uspješno učitana karta pića
         */
        public boolean ucitajKartuPica() {
            var nazivDatotekePica = this.konfig.dajPostavku("datotekaKartaPica");
            var datoteka = Path.of(nazivDatotekePica);
            if (!Files.exists(datoteka) || !Files.isRegularFile(datoteka) || !Files.isReadable(datoteka)) {
                return false;
            }
            try (BufferedReader citac = Files.newBufferedReader(datoteka)) {
                Gson gson = new Gson();
                var kartaPicaNiz = gson.fromJson(citac, KartaPica[].class);
                var kartaPicaTok = Arrays.stream(kartaPicaNiz);
                kartaPicaTok.forEach(kp -> this.kartaPica.put(kp.id(), kp));
            } catch (IOException ex) {
                return false;
            }
            return true;
        }

        /**
         * Obrada kraja rada.
         *
         * @param mreznaUticnica mrežna utičnica
         * @throws IOException ako dođe do greške
         */
        public void obradiKomanduKraj(Socket mreznaUticnica) throws IOException {
            boolean validKraj = false;
            try {
                BufferedReader citac =
                        new BufferedReader(new InputStreamReader(mreznaUticnica.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter pisac =
                        new PrintWriter(new OutputStreamWriter(mreznaUticnica.getOutputStream(), StandardCharsets.UTF_8), true);

                String linija = citac.readLine();
                mreznaUticnica.shutdownInput();

                Matcher mKraj = predlozakKraj.matcher(linija);
                Matcher mStart = predlozakStartKontrola.matcher(linija);
                Matcher mPauza = predlozakPauzaKontrola.matcher(linija);
                Matcher mStatus = predlozakStatusKontrola.matcher(linija);
                Matcher mSpavanje = predlozakSpavanjeKontrola.matcher(linija);
                Matcher mOsvjezi = predlozakOsvjeziKontrola.matcher(linija);
                Matcher mKrajWs = predlozakKrajWs.matcher(linija);

                if (mStatus.matches()) {
                    obradiStatus(mStatus, pisac);
                } else if (mPauza.matches()) {
                    obradiPauza(mPauza, pisac);
                } else if (mStart.matches()) {
                    obradiStart(mStart, pisac);
                } else if(mSpavanje.matches()) {
                    obradiSpavanje(mSpavanje, pisac);
                } else if (mOsvjezi.matches()) {
                    obradiOsvjezi(mOsvjezi, pisac);
                }else if (mKrajWs.matches()) {
                    validKraj = obradiKrajWs(mKrajWs, pisac);
                } else if (mKraj.matches()) {
                    validKraj = obradiKraj(mKraj, pisac);
                } else {
                    pisac.println("ERROR 10 - Format komande nije ispravan");
                }
            } catch (Exception e) {
                PrintWriter pisac =
                        new PrintWriter(new OutputStreamWriter(mreznaUticnica.getOutputStream(), StandardCharsets.UTF_8));
                pisac.println("ERROR 19 - Nešto drugo nije u redu.");
            } finally {
                try {
                    mreznaUticnica.shutdownOutput();
                } catch (IOException e) {
                }
                mreznaUticnica.close();
                evidentirajZatvorenuVezu();
                if (validKraj) {
                    this.kraj.set(true);
                }
            }
        }

        /**
         * Obrada Kraja posluzitelja.
         *
         * @param mKraj ulazni matcher
         * @param pisac    PrintWriter prema izvornom klijentu
         */
        private boolean obradiKraj(Matcher mKraj, PrintWriter pisac) {
            String kod = mKraj.group("kod");
            if (!kod.equals(this.kodZaKraj)) {
                pisac.println("ERROR 10 - Format komande nije ispravan ili nije ispravan kod za kraj");
                return false;
            }
            String komanda = "KRAJ " + kod;
            String odgovorKraj = posaljiKomanduPartnerima(komanda, pisac);
            if (!odgovorKraj.contains("OK")) {
                pisac.println(odgovorKraj);
                return false;
            }
            String restAdresa = this.konfig.dajPostavku("restAdresa");
            String odgovor = posaljiHeadKrajInfo(restAdresa);
            if (!odgovor.contains("OK")) {
                return false;
            }
            pisac.println("OK");
            return true;
        }

        /**
         * Obrada KRAJWS komande.
         *
         * @param mKraj ulazni matcher
         * @param pisac PrintWriter prema izvornom klijentu
         */
        private boolean obradiKrajWs(Matcher mKraj, PrintWriter pisac) {
            String kod = mKraj.group("kod");
            if (!kod.equals(this.kodZaKraj)) {
                pisac.println("ERROR 10 - Format komande nije ispravan ili nije ispravan kod za kraj");
                return false;
            }
            String komanda = "KRAJ " + kod;
            if (!posaljiKomanduPartnerima(komanda, pisac).contains("OK")) {
                pisac.println(posaljiKomanduPartnerima(komanda, pisac));
                return false;
            }
            pisac.println("OK");
            return true;
        }

        /**
         * Slanje zahtjeva HEAD kraj/info n aposluzitelj partner.
         *
         * @param restAdresa ulazni parametar rest adrese servisa
         */
        private String posaljiHeadKrajInfo(String restAdresa) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(restAdresa + "/kraj/info"))
                        .timeout(Duration.ofSeconds(3))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<Void> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.discarding()
                );

                if (response.statusCode() == 200) {
                    return "OK";
                } else {
                    return "ERROR 15 – Poslužitelj za partnere u pauzi";
                }

            } catch (IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
                return "ERROR 17 – RESTful zahtjev nije uspješan";
            }
        }

        /**
         * Šalje danu komandu svim partnerima i odmah prekida na prvoj grešci.
         * @param komanda String naredbe (npr. "KRAJWS ABBACABA")
         * @param pisac   PrintWriter prema izvornom klijentu
         * @return true ako su svi partneri vratili OK, false inače
         */
        private String posaljiKomanduPartnerima(String komanda, PrintWriter pisac) {
            boolean poslanoJednom = false;

            for (Partner partner : this.partneri.values()) {
                int port = partner.mreznaVrataKraj();
                try (Socket socket = new Socket("20.24.5.3", port)) {
                    poslanoJednom = true;
                    try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        out.println(komanda);
                        String odgovor = in.readLine();
                        if (odgovor == null || !odgovor.trim().equals("OK")) {
                            return "ERROR 14 – Barem jedan partner nije završio rad";
                        }
                    }
                } catch (IOException ex) {
                }
            }
            if (poslanoJednom) {
                return "OK";
            }
            return "OK";
        }

        /**
         * Obrada statusa poslužitelja.
         *
         * @param mStatus ulazni matcher
         * @param pisac   PrintWriter prema izvornom klijentu
         */
        private void obradiStatus(Matcher mStatus, PrintWriter pisac) {
            String kodAdmin = this.konfig.dajPostavku("kodZaAdminTvrtke");
            String kod = mStatus.group("kod");
            int id = Integer.parseInt(mStatus.group("id"));
            if (!kod.equals(kodAdmin)) {
                pisac.println("ERROR 12 - Pogrešan kodZaAdminTvrtke");
            } else if (!idPosluzitelja.containsKey(id)) {
                pisac.println("ERROR 13 - Pogrešna promjena pauze ili starta");
            } else {
                int stanje = idPosluzitelja.get(id).get() ? 1 : 0;
                pisac.println("OK " + stanje);
            }
        }

        /**
         * Obrada pauze ili starta poslužitelja.
         *
         * @param mPauza ulazni matcher
         * @param pisac  PrintWriter prema izvornom klijentu
         */
        private void obradiPauza(Matcher mPauza, PrintWriter pisac) {
            String kodAdmin = this.konfig.dajPostavku("kodZaAdminTvrtke");
            String kod = mPauza.group("kod");
            int id = Integer.parseInt(mPauza.group("id"));
            if (!kod.equals(kodAdmin)) {
                pisac.println("ERROR 12 - Pogrešan kodZaAdminTvrtke");
            } else if (!idPosluzitelja.containsKey(id)) {
                pisac.println("ERROR 13 - Pogrešna promjena pauze ili starta");
            } else if (!idPosluzitelja.get(id).get()) {
                pisac.println("ERROR 15 - Poslužitelj za partnere u pauzi");
            } else {
                idPosluzitelja.get(id).set(false);
                pisac.println("OK");
            }
        }

        /**
         * Obrada starta poslužitelja.
         *
         * @param mStart ulazni matcher
         * @param pisac  PrintWriter prema izvornom klijentu
         */
        private void obradiStart(Matcher mStart, PrintWriter pisac) {
            String kodAdmin = this.konfig.dajPostavku("kodZaAdminTvrtke");
            String kod = mStart.group("kod");
            int id = Integer.parseInt(mStart.group("id"));
            if (!kod.equals(kodAdmin)) {
                pisac.println("ERROR 12 - Pogrešan kodZaAdminTvrtke");
            } else if (!idPosluzitelja.containsKey(id) || idPosluzitelja.get(id).get()) {
                pisac.println("ERROR 13 - Pogrešna promjena pauze ili starta");
            } else {
                idPosluzitelja.get(id).set(true);
                pisac.println("OK");
            }
        }

        /**
         * Obrada spavanja poslužitelja.
         *
         * @param mSpavanje ulazni matcher
         * @param pisac     PrintWriter prema izvornom klijentu
         */
        private void obradiSpavanje(Matcher mSpavanje, PrintWriter pisac) {
            String kodAdmin = this.konfig.dajPostavku("kodZaAdminTvrtke");
            String kod = mSpavanje.group("kod");
            int ms = Integer.parseInt(mSpavanje.group("milisekunde"));
            if (!kod.equals(kodAdmin)) {
                pisac.println("ERROR 12 - Pogrešan kodZaAdminTvrtke");
            } else {
                try {
                    Thread.sleep(ms);
                    pisac.println("OK\n");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    pisac.println("ERROR 16 – Prekid spavanja dretve");
                }
            }
        }

        /**
         * Obrada osvježavanja podataka.
         *
         * @param mOsvjezi ulazni matcher
         * @param pisac    PrintWriter prema izvornom klijentu
         */
        private void obradiOsvjezi(Matcher mOsvjezi, PrintWriter pisac) {
            String kodAdmin = this.konfig.dajPostavku("kodZaAdminTvrtke");
            String kod = mOsvjezi.group("kod");
            if (!kod.equals(kodAdmin)) {
                pisac.println("ERROR 12 - Pogrešan kodZaAdminTvrtke");
            } else if (!idPosluzitelja.get(1).get() || !idPosluzitelja.get(2).get()) {
                pisac.println("ERROR 15 – Poslužitelj za partnere u pauzi");
            } else {
                try {
                    ucitajPartnereIzDatoteke();
                    ucitajKartuPica();
                    ucitajJelovnike();
                    pisac.println("OK");
                } catch (Exception ex) {
                    pisac.println("Neuspjelo osvježavanje: " + ex.getMessage());
                }
            }
        }
        /**
         * Ucitaj konfiguraciju.
         *
         * @param nazivDatoteke naziv datoteke konfiguracije
         * @return true, ako je uspješno učitavanje konfiguracije
         */
        public boolean ucitajKonfiguraciju(String nazivDatoteke) {
            try {
                this.konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
                return true;
            } catch (NeispravnaKonfiguracija ex) {
            }
            return false;
        }

        /**
         * Obrada popisa partnera.
         *
         * @return odgovor
         */
        public String obradiPopis() {
            var partnerPopisLista = this.partneri.values().stream()
                    .map(partner -> new PartnerPopis(
                            partner.id(),
                            partner.naziv(),
                            partner.vrstaKuhinje(),
                            partner.adresa(),
                            partner.mreznaVrata(),
                            partner.gpsSirina(),
                            partner.gpsDuzina()
                    )).toList();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonPartnera = gson.toJson(partnerPopisLista);

            return "OK\n" + jsonPartnera;
        }

        /**
         * Obrada registracije partnera.
         *
         * @param m ulazni matcher
         * @return odgovor
         */
        public String obradiRegistraciju(Matcher m) {
            int id = Integer.parseInt(m.group("id"));
            String naziv = m.group("naziv");
            String kodZaAdmin = m.group("adminKod");
            String vrstaKuhinje = m.group("vrstaKuhinje");
            String adresa = m.group("adresa");
            int mreznaVrata = Integer.parseInt(m.group("mreznaVrata"));
            int mreznaVrataKraj = Integer.parseInt(m.group("mreznaVrataKrajPartnera"));
            float gpsSirina = Float.parseFloat(m.group("gpsSirina"));
            float gpsDuzina = Float.parseFloat(m.group("gpsDuzina"));

            if (this.partneri.get(id) != null) {
                return "ERROR 21 - Već postoji partner s id u kolekciji partnera";
            }

            String kombinacija = naziv + adresa;
            String sigurnosniKod = Integer.toHexString(kombinacija.hashCode());

            var novi = new Partner(id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, kodZaAdmin);
            this.partneri.put(novi.id(), novi);
            azurirajDatotekuPartnera();

            return "OK " + sigurnosniKod;
        }

        /**
         * Učitava kartu pića iz datoteke.
         *
         * @return true ako je uspješno učitana karta pića
         */
        private void azurirajDatotekuPartnera() {
            var nazivDatotekePartnera = this.konfig.dajPostavku("datotekaPartnera");
            Path datotekaPartnera = Path.of(nazivDatotekePartnera);
            try (var bw = Files.newBufferedWriter(datotekaPartnera)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(partneri.values(), bw);
            } catch (IOException ex) {
            }
        }

        /**
         * Učitava partnere iz datoteke.
         *
         * @return true ako su partneri učitani
         */
        private boolean ucitajPartnereIzDatoteke() {
            var nazivDatotekePartnera = this.konfig.dajPostavku("datotekaPartnera");
            Path datotekaPartnera = Path.of(nazivDatotekePartnera);
            if (!Files.exists(datotekaPartnera) || !Files.isRegularFile(datotekaPartnera) || !Files.isReadable(datotekaPartnera)) {
                return false;
            }
            try (BufferedReader citac= Files.newBufferedReader(datotekaPartnera)) {
                Gson gson = new Gson();
                var partnerNiz = gson.fromJson(citac, Partner[].class);
                if (partnerNiz == null) {
                    return false;
                }
                Arrays.stream(partnerNiz).forEach(pr -> this.partneri.put(pr.id(), pr));
                return true;
            } catch (IOException ex) {
                return false;
            }
        }
    }

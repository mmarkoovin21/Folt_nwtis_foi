package org.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3.rest;

import edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3.dao.KorisnikDAO;
import edu.unizg.foi.nwtis.podaci.Korisnik;
import edu.unizg.foi.nwtis.podaci.Narudzba;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * REST resurs za komunikaciju s partnerskim serverom i upravljanje korisnicima.
 */
@Path("api/partner")
public class PartnerResource {
    @Inject
    @ConfigProperty(name = "adresaPartner")
    private String adresaPartner;
    @Inject
    @ConfigProperty(name = "mreznaVrataKrajPartner")
    private String mreznaVrataKrajPartner;
    @Inject
    @ConfigProperty(name = "mreznaVrataRadPartner")
    private String mreznaVrataRadPartner;
    @Inject
    @ConfigProperty(name = "kodZaAdminPartnera")
    private String kodZaAdminPartnera;
    @Inject
    @ConfigProperty(name = "kodZaKraj")
    private String kodZaKraj;
    @Inject
    RestConfiguration restConfiguration;

    /**
     * Provjera dosega partnerskog poslužitelja (HEAD).
     *
     * @return HTTP 200 ako je poslužitelj dostupan, inače HTTP 500.
     */
    @HEAD
    @Operation(summary = "Provjera statusa poslužitelja partner")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")})
    @Counted(name = "brojZahtjeva_", description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_headPosluzitelj", description = "Vrijeme trajanja metode")
    public Response headPosluzitelj() {
        var status = posaljiKomandu("KRAJ xxx", this.mreznaVrataKrajPartner);
        if (status != null) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Postavlja dio poslužitelja u rad (HEAD).
     *
     * @param id ID dijela poslužitelja koji se pokreće.
     * @return HTTP 200 ako je uspješno, inače HTTP 204.
     */
    @Path("start/{id}")
    @HEAD
    @Operation(summary = "Postavljanje dijela poslužitelja partner u rad")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
    @Counted(name = "brojZahtjeva_headPosluziteljStart",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_headPosluziteljStart", description = "Vrijeme trajanja metode")
    public Response headPosluziteljStart(@PathParam("id") int id) {
        var status = posaljiKomandu("START " + this.kodZaAdminPartnera + " " + id, this.mreznaVrataKrajPartner);
        if (status != null && status.contains("OK")) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    /**
     * Provjera statusa dijela poslužitelja (HEAD).
     *
     * @param id ID dijela poslužitelja.
     * @return HTTP 200 ako je aktivan, inače HTTP 204.
     */
    @Path("status/{id}")
    @HEAD
    @Operation(summary = "Provjera statusa dijela poslužitelja partner")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
    @Counted(name = "brojZahtjeva_eadPosluziteljStatus",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_eadPosluziteljStatus", description = "Vrijeme trajanja metode")
    public Response headPosluziteljStatus(@PathParam("id") int id) {
        var status = posaljiKomandu("STATUS " + this.kodZaAdminPartnera + " " + id, this.mreznaVrataKrajPartner);
        if (status != null && status.contains("OK 1")) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    /**
     * Postavlja dio poslužitelja u pauzu (HEAD).
     *
     * @param id ID dijela poslužitelja.
     * @return HTTP 200 ako je uspješno, inače HTTP 204.
     */
    @Path("pauza/{id}")
    @HEAD
    @Operation(summary = "Postavljanje dijela poslužitelja partner u pauzu")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
    @Counted(name = "brojZahtjeva_headPosluziteljPauza",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_headPosluziteljPauza", description = "Vrijeme trajanja metode")
    public Response headPosluziteljPauza(@PathParam("id") int id) {
        var status = posaljiKomandu("PAUZA " + this.kodZaAdminPartnera + " " + id, this.mreznaVrataKrajPartner);
        if (status != null && status.contains("OK")) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    /**
     * Zaustavlja cijeli poslužitelj (HEAD).
     *
     * @return HTTP 200 ako je uspješno, inače HTTP 204.
     */
    @Path("kraj")
    @HEAD
    @Operation(summary = "Zaustavljanje poslužitelja partner")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
    @Counted(name = "brojZahtjeva_headPosluziteljKraj",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_headPosluziteljKraj", description = "Vrijeme trajanja metode")
    public Response headPosluziteljKraj() {
        var status = posaljiKomandu("KRAJ " + this.kodZaKraj, this.mreznaVrataKrajPartner);
        if (status != null && status.contains("OK")) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    /**
     * Dohvaća kartu pića za korisnika (GET).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @return JSON karta pića uz HTTP 200 ili odgovarajući status.
     */
    @Path("kartapica")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Dohvat karte pica")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "401", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")})
    @Counted(name = "brojZahtjeva_getPartneri",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_getKartapica", description = "Vrijeme trajanja metode")
    public Response getKartapica(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka
    ) {
        Response auth = provjeriPrijavu(korisnik, lozinka);
        if (auth != null) return auth;
        String komanda = "KARTAPIĆA " + korisnik;
        String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
        if (odgovor != null && odgovor.contains("OK")) {
            String jsonOnly = odgovor.replaceFirst("(?m)^OK\\s*", "");
            return Response.ok(jsonOnly).status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dohvaća jelovnik za korisnika (GET).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @return JSON jelovnik uz HTTP 200 ili odgovarajući status.
     */
    @Path("jelovnik")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Dohvat jelovnika")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "401", description = "Neautorizirano"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")})
    @Counted(name = "brojZahtjeva_getJelovnik",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_getJelovnik", description = "Vrijeme trajanja metode")
    public Response getJelovnik(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka
    ) {
        Response auth = provjeriPrijavu(korisnik, lozinka);
        if (auth != null) return auth;
        String komanda = "JELOVNIK " + korisnik;
        String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
        if (odgovor != null && odgovor.contains("OK")) {
            String jsonOnly = odgovor.replaceFirst("(?m)^OK\\s*", "");
            return Response.ok(jsonOnly).status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dohvaća stanje narudžbe korisnika (GET).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @return JSON stanje narudžbe uz HTTP 200 ili odgovarajući status.
     */
    @Path("narudzba")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Dohvat narudzbe")
    @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "401", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")})
    @Counted(name = "brojZahtjeva_getNarudzba",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_getNarudzba", description = "Vrijeme trajanja metode")
    public Response getNarudzba(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka
    ) {
        Response auth = provjeriPrijavu(korisnik, lozinka);
        if (auth != null) return auth;
        String komanda = "STANJE " + korisnik + "\n";
        String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
        String jsonOnly = odgovor.replaceFirst("(?m)^OK\\s*", "");
        if (odgovor.contains("OK")) {
            return Response.ok(jsonOnly).status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Postavljanje nove narudžbe za korisnika (POST).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @return HTTP 201 ako je kreirana, HTTP 409 ako postoji, HTTP 401 ako neautorizirano ili HTTP 500.
     */
    @Path("narudzba")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Zadavanje narudžbe")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Uspješna operacija"),
            @APIResponse(responseCode = "401", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "409", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    @Counted(name = "brojZahtjeva_postNarudzba", description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_postNarudzba", description = "Vrijeme trajanja metode")
    public Response postNarudzba(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka
    ) {
        try {
            Response auth = provjeriPrijavu(korisnik, lozinka);
            if (auth != null) return auth;
            String komanda = "NARUDŽBA " + korisnik + "\n";
            String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
            return odgovor.contains("OK")
                    ? Response.status(Response.Status.CREATED).build()
                    : Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dodavanje jela u postojeću narudžbu (POST).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @param jelo     Podaci o jelima (ID i količina).
     * @return HTTP 201 ako je uspješno, HTTP 409 ako konflikt, HTTP 401 ili HTTP 500.
     */
    @Path("jelo")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postJelo(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka,
            Narudzba jelo
    ) {
        try {
            Response auth = provjeriPrijavu(korisnik, lozinka);
            if (auth != null) return auth;
            String komanda = "JELO " + korisnik + " " + jelo.id() + " " + jelo.kolicina() + "\n";
            String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
            return odgovor.contains("OK")
                    ? Response.status(Response.Status.CREATED).build()
                    : Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Dodavanje pića u postojeću narudžbu (POST).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @param pice     Podaci o pićima (ID i količina).
     * @return HTTP 201 ako je uspješno, HTTP 409 ako konflikt, HTTP 401 ili HTTP 500.
     */
    @Path("pice")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postPice(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka,
            Narudzba pice
    ) {
        try {
            Response auth = provjeriPrijavu(korisnik, lozinka);
            if (auth != null) return auth;
            String komanda = "PIĆE " + korisnik + " " + pice.id() + " " + pice.kolicina() + "\n";
            String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
            return odgovor.contains("OK")
                    ? Response.status(Response.Status.CREATED).build()
                    : Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Zahtjev za izdavanje računa (POST).
     *
     * @param korisnik Korisničko ime za autentikaciju.
     * @param lozinka  Lozinka za autentikaciju.
     * @return HTTP 201 ako je uspješno, HTTP 409 ako konflikt, HTTP 401 ili HTTP 500.
     */
    @Path("racun")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Zahtjev za računom")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Uspješna operacija"),
            @APIResponse(responseCode = "401", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "409", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    @Counted(name = "brojZahtjeva_postRacun", description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_postRacun", description = "Vrijeme trajanja metode")
    public Response postRacun(
            @HeaderParam("korisnik") String korisnik,
            @HeaderParam("lozinka") String lozinka
    ) {
        try {
            Response auth = provjeriPrijavu(korisnik, lozinka);
            if (auth != null) return auth;
            String komanda = "RAČUN " + korisnik + "\n";
            String odgovor = posaljiKomandu(komanda, this.mreznaVrataRadPartner);
            return odgovor.contains("OK")
                    ? Response.status(Response.Status.CREATED).build()
                    : Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dohvat svih korisnika iz baze (GET).
     *
     * @return Lista korisnika uz HTTP 200 ili HTTP 500 kod pogreške.
     */
    @Path("korisnik")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat svih korisnika")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    @Counted(name = "brojZahtjeva_getkorisnik", description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_getkorisnik", description = "Vrijeme trajanja metode")
    public Response getkorisnik() {
        try (var vezaBP = this.restConfiguration.dajVezu()) {
            var korisnikDao = new KorisnikDAO(vezaBP);
            var partner = korisnikDao.dohvatiSve();
            return (partner != null)
                    ? Response.ok(partner).build()
                    : Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dohvat korisnika po ID-ju (GET).
     *
     * @param korisnik Korisničko ime korisnika.
     * @return Korisnik uz HTTP 200 ili HTTP 404/500.
     */
    @Path("korisnik/{korisnik}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat korisnika s ID-jem")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "404", description = "Ne postoji resurs"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    @Counted(name = "brojZahtjeva_getkorisnikId", description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_getkorisnikId", description = "Vrijeme trajanja metode")
    public Response getkorisnikId(@PathParam("korisnik") String korisnik) {
        try (var vezaBP = this.restConfiguration.dajVezu()) {
            var korisnikDao = new KorisnikDAO(vezaBP);
            var partner = korisnikDao.dohvati(korisnik, null, false);
            return (partner != null)
                    ? Response.ok(partner).build()
                    : Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Unos novog korisnika u bazu (POST).
     *
     * @param korisnik Objekt korisnika za unos.
     * @return HTTP 201 ako je kreirano, HTTP 409 ako konflikt ili HTTP 500.
     */
    @Path("korisnik")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Unos jednog partnera")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Uspješna kreiran resurs"),
            @APIResponse(responseCode = "409", description = "Već postoji resurs ili druga pogreška"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    @Counted(name = "brojZahtjeva_postKorisnik", description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_postKorisnik", description = "Vrijeme trajanja metode")
    public Response postKorisnik(Korisnik korisnik) {
        try (var vezaBP = this.restConfiguration.dajVezu()) {
            var korisnikDao = new KorisnikDAO(vezaBP);
            boolean status = korisnikDao.dodaj(korisnik);
            return status
                    ? Response.status(Response.Status.CREATED).build()
                    : Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pauza rada poslužitelja na zadano vrijeme (GET).
     *
     * @param trajanjeSpavanjaMilis Vrijeme spavanja u milisekundama.
     * @return HTTP 200 ako je poslužitelj uspješno pauziran, inače HTTP 500.
     */
    @Path("spava")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Spavanje servera")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Server spava"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    public Response getSpavanje(@QueryParam("vrijeme") int trajanjeSpavanjaMilis) {
        String komanda = "SPAVA " + this.kodZaAdminPartnera + " " + trajanjeSpavanjaMilis;
        String odgovor = posaljiKomandu(komanda, this.mreznaVrataKrajPartner);
        return (odgovor != null && odgovor.contains("OK"))
                ? Response.status(Response.Status.OK).build()
                : Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Provjerava autentičnost korisnika.
     *
     * @param korisnik Korisničko ime.
     * @param lozinka  Lozinka.
     * @return null ako je autentikacija uspješna, inače odgovarajući Response.
     */
    private Response provjeriPrijavu(String korisnik, String lozinka) {
        try (var vezaBP = this.restConfiguration.dajVezu()) {
            var korisnikDAO = new KorisnikDAO(vezaBP);
            Korisnik k = korisnikDAO.dohvati(korisnik, lozinka, true);
            return (k == null)
                    ? Response.status(Response.Status.UNAUTHORIZED).build()
                    : null;
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Šalje komandu partnerskom serveru preko socket veze.
     *
     * @param komanda     Tekst komande.
     * @param mreznaVrata Vrata za povezivanje.
     * @return Odgovor servera ili null kod pogreške.
     */
    private String posaljiKomandu(String komanda, String mreznaVrata) {
        try (Socket mreznaUticnica = new Socket(this.adresaPartner,
                Integer.parseInt(mreznaVrata));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(mreznaUticnica.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(mreznaUticnica.getOutputStream(), StandardCharsets.UTF_8), true)) {

            out.println(komanda);
            mreznaUticnica.shutdownOutput();
            StringBuilder odgovor = new StringBuilder();
            String linija;
            while ((linija = in.readLine()) != null) {
                odgovor.append(linija).append("\n");
            }
            return odgovor.toString().trim();
        } catch (IOException e) {
            return null;
        }
    }
}
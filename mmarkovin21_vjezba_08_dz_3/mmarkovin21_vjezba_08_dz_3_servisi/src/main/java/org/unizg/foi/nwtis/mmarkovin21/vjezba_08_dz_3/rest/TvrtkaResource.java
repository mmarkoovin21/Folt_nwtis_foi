package org.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3.rest;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3.dao.PartnerDAO;
import edu.unizg.foi.nwtis.podaci.Jelovnik;
import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;
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
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RESTful Web servis za obradu zahtjeva za tvrtkz.
 * Pruža krajnje točke za status poslužitelja, upravljanje partnerima i preuzimanje izbornika.
 */
@Path("api/tvrtka")
public class TvrtkaResource {
  @Inject
  @ConfigProperty(name = "adresa")
  private String tvrtkaAdresa;
  @Inject
  @ConfigProperty(name = "mreznaVrataKraj")
  private String mreznaVrataKraj;
  @Inject
  @ConfigProperty(name = "mreznaVrataRegistracija")
  private String mreznaVrataRegistracija;
  @Inject
  @ConfigProperty(name = "mreznaVrataRad")
  private String mreznaVrataRad;
  @Inject
  @ConfigProperty(name = "kodZaAdminTvrtke")
  private String kodZaAdminTvrtke;
  @Inject
  @ConfigProperty(name = "kodZaKraj")
  private String kodZaKraj;
  @Inject
  RestConfiguration restConfiguration;

    /**
     * Mapa za pohranu partnera po njihovom ID.
     * Koristi se za keširanje podataka partnera za brzi pristup.
     */
  private final Map<Integer, Partner> partneri = new HashMap<>();

  /**
   * Provjerava status poslužitelja tvrtka.
   * @return HTTP odgovor sa statusom 200 OK ako je poslužitelj aktivan, inače 500 Internal Server Error.
   */
  @HEAD
  @Operation(summary = "Provjera statusa poslužitelja tvrtka")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_", description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_headPosluzitelj", description = "Vrijeme trajanja metode")
  public Response headPosluzitelj() {
    var status = posaljiKomandu("KRAJ xxx", this.mreznaVrataKraj);
    if (status != null) {
      return Response.status(Response.Status.OK).build();
    } else {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Postavlja dijelove poslužitelja tvrtka u rad.
   * @param id ID dijela poslužitelja koji se pokreće.
   * @return HTTP odgovor sa statusom 200 OK ako je operacija uspješna, inače 204 No Content.
   */
  @Path("start/{id}")
  @HEAD
  @Operation(summary = "Postavljanje dijela poslužitelja tvrtka u rad")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
  @Counted(name = "brojZahtjeva_headPosluziteljStart",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_headPosluziteljStart", description = "Vrijeme trajanja metode")
  public Response headPosluziteljStart(@PathParam("id") int id) {
    var status = posaljiKomandu("START " + this.kodZaAdminTvrtke + " " + id, this.mreznaVrataKraj);
    if (status != null && status.contains("OK")) {
      return Response.status(Response.Status.OK).build();
    } else {
      return Response.status(Response.Status.NO_CONTENT).build();
    }
  }

    /**
     * Provjerava status dijela poslužitelja tvrtka.
     * @param id ID dijela poslužitelja čiji se status provjerava.
     * @return HTTP odgovor sa statusom 200 OK ako je dio poslužitelja aktivan, inače 204 No Content.
     */
  @Path("status/{id}")
  @HEAD
  @Operation(summary = "Provjera statusa dijela poslužitelja tvrtka")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
  @Counted(name = "brojZahtjeva_eadPosluziteljStatus",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_eadPosluziteljStatus", description = "Vrijeme trajanja metode")
  public Response headPosluziteljStatus(@PathParam("id") int id) {
    var status = posaljiKomandu("STATUS " + this.kodZaAdminTvrtke + " " + id, this.mreznaVrataKraj);
    if (status != null && status.contains("OK 1")) {
      return Response.status(Response.Status.OK).build();
    } else {
      return Response.status(Response.Status.NO_CONTENT).build();
    }
  }

    /**
     * Postavlja dijelove poslužitelja tvrtka u pauzu.
     * @param id ID dijela poslužitelja koji se stavlja u pauzu.
     * @return HTTP odgovor sa statusom 200 OK ako je operacija uspješna, inače 204 No Content.
     */
  @Path("pauza/{id}")
  @HEAD
  @Operation(summary = "Postavljanje dijela poslužitelja tvrtka u pauzu")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
  @Counted(name = "brojZahtjeva_headPosluziteljPauza",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_headPosluziteljPauza", description = "Vrijeme trajanja metode")
  public Response headPosluziteljPauza(@PathParam("id") int id) {
    var status = posaljiKomandu("PAUZA " + this.kodZaAdminTvrtke + " " + id, this.mreznaVrataKraj);
    if (status != null && status.contains("OK")) {
      return Response.status(Response.Status.OK).build();
    } else {
      return Response.status(Response.Status.NO_CONTENT).build();
    }
  }

    /**
     * Zaustavlja poslužitelj tvrtka.
     * @return HTTP odgovor sa statusom 200 OK ako je operacija uspješna, inače 204 No Content.
     */
  @Path("kraj")
  @HEAD
  @Operation(summary = "Zaustavljanje poslužitelja tvrtka")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
  @Counted(name = "brojZahtjeva_headPosluziteljKraj",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_headPosluziteljKraj", description = "Vrijeme trajanja metode")
  public Response headPosluziteljKraj() {
    var status = posaljiKomandu("KRAJ " + this.kodZaKraj, this.mreznaVrataKraj);
    if (status != null && status.contains("OK")) {
      return Response.status(Response.Status.OK).build();
    } else {
      return Response.status(Response.Status.NO_CONTENT).build();
    }
  }

    /**
     * Informira o zaustavljanju poslužitelja tvrtka.
     * Ova metoda se koristi za obavještavanje korisnika o završetku rada poslužitelja.
     * @return HTTP odgovor sa statusom 200 OK ako je operacija uspješna, inače 204 No Content.
     */
  @Path("kraj/info")
  @HEAD
  @Operation(summary = "Informacija o zaustavljanju poslužitelja tvrtka")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "204", description = "Pogrešna operacija")})
  @Counted(name = "brojZahtjeva_headPosluziteljKrajInfo",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_headPosluziteljKrajInfo", description = "Vrijeme trajanja metode")
  public Response headPosluziteljKrajInfo() {
    System.out.println("PosluziteljTvrtka je završio rad.");
    return Response.status(Response.Status.OK).build();
  }

    /**
     * Dohvaća kartu pica.
     * Ova metoda šalje zahtjev za kartu pica i vraća odgovor u JSON formatu.
     * @return HTTP odgovor sa statusom 200 OK i JSON podacima karte pica, ili 500 Internal Server Error u slučaju greške.
     */
  @Path("kartapica")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Dohvat karte pica")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_getPartneri",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_getPartneri", description = "Vrijeme trajanja metode")
  public Response getKartapica() {
    try (var vezaBP = this.restConfiguration.dajVezu()) {
      var partnerDAO = new PartnerDAO(vezaBP);
      var partner = partnerDAO.dohvatiPrvog(false);
      if (partner != null) {
        String komanda = "KARTAPIĆA " + partner.id() + " " + partner.sigurnosniKod();
        String odgovor = posaljiKomandu(komanda, this.mreznaVrataRad);
          assert odgovor != null;
          String jsonOnly = odgovor.replaceFirst("(?m)^OK\\s*", "");
        return Response.ok(jsonOnly).status(Response.Status.OK).build();
      } else {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

    /**
     * Dohvaća jelovnik svih aktivnih partnera.
     * Ova metoda šalje zahtjev za jelovnik i vraća odgovor u JSON formatu.
     * @return HTTP odgovor sa statusom 200 OK i JSON podacima jelovnika, ili 500 Internal Server Error u slučaju greške.
     */
    @Path("jelovnik")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat svih jelovnika")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Uspješna operacija"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    @Counted(name = "brojZahtjeva_getJelovnik",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_getJelovnik", description = "Vrijeme trajanja metode")
    public Response getJelovnik() {
      try (var vezaBP = this.restConfiguration.dajVezu()) {
        var partnerDAO = new PartnerDAO(vezaBP);
        Map<String, List<Jelovnik>> jelovnici = new HashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Jelovnik>>() {}.getType();
        for (String kodKuhinje : List.of("MK", "KK", "VK")) {
          Partner partner = partnerDAO.dohvatiKuhinju(kodKuhinje, false);
          if (partner == null) {
            continue;
          }
          String komanda = "JELOVNIK " + partner.id() + " " + partner.sigurnosniKod();
          String odgovor = posaljiKomandu(komanda, this.mreznaVrataRad);

          if (odgovor == null || !odgovor.startsWith("OK")) {
            continue;
          }
          String jsonArray = odgovor.replaceFirst("(?m)^OK\\s*", "");
          List<Jelovnik> lista = gson.fromJson(jsonArray, listType);
          jelovnici.put(kodKuhinje, lista);
        }
        if (jelovnici.isEmpty()) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jelovnici).build();
      } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    }

  /**
     * Dohvaća jelovnik partnera po ID-u.
     * Ova metoda šalje zahtjev za jelovnik određenog partnera i vraća odgovor u JSON formatu.
     * @param id ID partnera čiji se jelovnik dohvaća.
     * @return HTTP odgovor sa statusom 200 OK i JSON podacima jelovnika, ili 404 Not Found ako partner ne postoji,
     * ili 500 Internal Server Error u slučaju greške.
     */
  @Path("jelovnik/{id}")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Dohvat jelovnika partnera")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "404", description = "Ne postoji resurs"),
          @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_getPartneri",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_getPartneri", description = "Vrijeme trajanja metode")
  public Response getjelovnikPartnera(@PathParam("id") int id) {
    try (var vezaBP = this.restConfiguration.dajVezu()) {
      var partnerDAO = new PartnerDAO(vezaBP);
      var partner = partnerDAO.dohvati(id,false);
      if (partner != null ) {
        String komanda = "JELOVNIK " + partner.id() + " " + partner.sigurnosniKod();
        String jsonString = posaljiKomandu(komanda, this.mreznaVrataRad).replaceFirst("(?m)^OK\\s*", "");
        return Response.ok(jsonString).status(Response.Status.OK).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

    /**
     * Dohvaća sve partnere.
     * Ova metoda vraća listu svih partnera u JSON formatu.
     * @return HTTP odgovor sa statusom 200 OK i listom partnera, ili 500 Internal Server Error u slučaju greške.
     */
  @Path("partner")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Dohvat svih partnera")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_getPartneri",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_getPartneri", description = "Vrijeme trajanja metode")
  public Response getPartneri() {
    try (var vezaBP = this.restConfiguration.dajVezu()) {
      var partnerDAO = new PartnerDAO(vezaBP);
      var partneri = partnerDAO.dohvatiSve(true);
      return Response.ok(partneri).status(Response.Status.OK).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

    /**
     * Provjerava točnost partnera u bazi i na serveru tvrtka.
     * Ova metoda šalje zahtjev za popis partnera i vraća listu partnera koji su u bazi podataka.
     * @return HTTP odgovor sa statusom 200 OK i listom partnera, ili 500 Internal Server Error u slučaju greške.
     */
  @Path("partner/provjera")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Dohvat svih partnera")
  @APIResponses(value = {
          @APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_getPartneri",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_getPartneri", description = "Vrijeme trajanja metode")
  public Response getPartneriIzDatoteke() {
    try (var vezaBP = this.restConfiguration.dajVezu()) {
      var partnerDAO = new PartnerDAO(vezaBP);
        String komanda = "POPIS";
        String jsonString = posaljiKomandu(komanda, this.mreznaVrataRegistracija).replaceFirst("(?m)^OK\\s*", "");
      napraviObjektPartneri(jsonString);
      var partneri = partnerDAO.dohvatiSve(true);
        if (partneri != null) {
          List<Partner> zajednicki = partneri.stream()
                  .filter(p -> this.partneri.containsKey(p.id()))
                  .toList();
          return Response.ok(zajednicki).status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Dohvaća partnera iz baze.
   * Ova metoda šalje zahtjev za jednog partnera s ID parametrom iz zahtjeva i vraća partnera koji je bazi podataka.
   * @return HTTP odgovor sa statusom 200 OK objektom partnera, ili 500 Internal Server Error u slučaju greške.
   */
  @Path("partner/{id}")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Dohvat jednog partnera")
  @APIResponses(value = {@APIResponse(responseCode = "200", description = "Uspješna operacija"),
          @APIResponse(responseCode = "404", description = "Ne postoji resurs"),
          @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_getPartner",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_getPartner", description = "Vrijeme trajanja metode")
  public Response getPartner(@PathParam("id") int id) {
    try (var vezaBP = this.restConfiguration.dajVezu()) {
      var partnerDAO = new PartnerDAO(vezaBP);
      var partner = partnerDAO.dohvati(id, true);
      if (partner != null) {
        return Response.ok(partner).status(Response.Status.OK).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  /**
   * Sprema partnera u bazu.
   * Ova metoda šalje POST zahtjev na Posluzitelj partner server te sprema json iz tjela zahtjeva u bazu.
   * @return HTTP odgovor sa statusom 200 OK, status 409 ako dođe do konflikta, ili 500 Internal Server Error u slučaju greške.
   */
  @Path("partner")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Unos jednog partnera")
  @APIResponses(
          value = {@APIResponse(responseCode = "201", description = "Uspješna kreiran resurs"),
                  @APIResponse(responseCode = "409", description = "Već postoji resurs ili druga pogreška"),
                  @APIResponse(responseCode = "500", description = "Interna pogreška")})
  @Counted(name = "brojZahtjeva_postPartner",
          description = "Koliko puta je pozvana operacija servisa")
  @Timed(name = "trajanjeMetode_postPartner", description = "Vrijeme trajanja metode")
  public Response postPartner(Partner partner) {
    try (var vezaBP = this.restConfiguration.dajVezu()) {
      var partnerDAO = new PartnerDAO(vezaBP);
      var status = partnerDAO.dodaj(partner);
      if (status) {
        return Response.status(Response.Status.CREATED).build();
      } else {
        return Response.status(Response.Status.CONFLICT).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Unosi listu obračuna u bazu podataka.
   * @param obracuni Lista objekata Obracun koji se unose.
   * @return HTTP 201 ako su svi obračuni uneseni, HTTP 500 kod pogreške.
   */
    @Path("obracun")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Unos obracuna")
    @APIResponses(
            value = {@APIResponse(responseCode = "201", description = "Uspješna kreiran resurs"),
                    @APIResponse(responseCode = "500", description = "Interna pogreška")})
    @Counted(name = "brojZahtjeva_postObracun",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_postObracun", description = "Vrijeme trajanja metode")
    public Response postObracun(List<Obracun> obracuni) {
      try (var vezaBP = this.restConfiguration.dajVezu()) {
        var dao = new PartnerDAO(vezaBP);
        vezaBP.setAutoCommit(false);
        try {
          for (Obracun o : obracuni) {
            if (!dao.dodajObracun(o)) {
              vezaBP.rollback();
              return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                      .build();
            }
          }
          vezaBP.commit();
          return Response.status(Response.Status.CREATED).build();
        } catch (SQLException e) {
          vezaBP.rollback();
          throw e;
        }
      } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .build();
      }
    }

  /**
   * Unosi listu obračuna i prosljeđuje ih tvrtki putem WS komande OBRAČUNWS.
   * @param obracuni Lista objekata Obracun koji se unose i šalju.
   * @return HTTP 201 ako je operacija uspješna, HTTP 500 kod pogreške.
   */
    @Path("obracun/ws")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(summary = "Unos obracuna")
    @APIResponses(
            value = {@APIResponse(responseCode = "201", description = "Uspješna kreiran resurs"),
                    @APIResponse(responseCode = "500", description = "Interna pogreška")})
    @Counted(name = "brojZahtjeva_postObracun",
            description = "Koliko puta je pozvana operacija servisa")
    @Timed(name = "trajanjeMetode_postObracun", description = "Vrijeme trajanja metode")
    public Response postObracunWs(List<Obracun> obracuni) {
      try (var vezaBP = this.restConfiguration.dajVezu()) {
        var dao = new PartnerDAO(vezaBP);
        vezaBP.setAutoCommit(false);
        try {
          for (Obracun o : obracuni) {
            if (!dao.dodajObracun(o)) {
              vezaBP.rollback();
              return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                      .build();
            }
          }
          vezaBP.commit();
          int partnerId = (obracuni.getFirst().partner());
          var sigKod = dao.dohvati(obracuni.getFirst().partner(), false).sigurnosniKod();
          Gson gson = new GsonBuilder()
                  .disableHtmlEscaping()
                  .create();
          String jsonArray = gson.toJson(obracuni);
          String komanda = "OBRAČUNWS "
                  + partnerId + " "
                  + sigKod + "\n"
                  + jsonArray;
          String odgovor = posaljiKomandu(komanda, this.mreznaVrataRad);
          if(odgovor.contains("OK")) {
            return Response.status(Response.Status.CREATED).build();
          } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
          }
        } catch (SQLException e) {
          vezaBP.rollback();
          throw e;
        }
      } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .build();
      }
    }

  /**
   * Dohvaća obračune za jela unutar danog vremenskog raspona.
   * @param epochSecondsOd Početno vrijeme u sekundama od epohe.
   * @param epochSecondsDo Krajnje vrijeme u sekundama od epohe.
   * @return Lista obračuna za jela iz zadanog intervala.
   */
    @Path("obracun/jelo")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat obračuna po vremenskom rasponu")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista obračuna"),
            @APIResponse(responseCode = "400", description = "Neispravan format parametara"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    public Response getObracunJelo(
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    ) {
      try (var vezaBP = restConfiguration.dajVezu()) {
        var dao = new PartnerDAO(vezaBP);
        List<Obracun> lista = dao.getObracuniJeloILIPice(true, epochSecondsOd, epochSecondsDo);
        return Response.ok(lista).build();
      } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    }

  /**
   * Dohvaća obračune za pića unutar danog vremenskog raspona.
   * @param epochSecondsOd Početno vrijeme u sekundama od epohe.
   * @param epochSecondsDo Krajnje vrijeme u sekundama od epohe.
   * @return Lista obračuna za pića iz zadanog intervala.
   */
    @Path("obracun/pice")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat obračuna po vremenskom rasponu")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista obračuna"),
            @APIResponse(responseCode = "400", description = "Neispravan format parametara"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    public Response getObracunPice(
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    ) {
      try (var vezaBP = restConfiguration.dajVezu()) {
        var dao = new PartnerDAO(vezaBP);
        List<Obracun> lista = dao.getObracuniJeloILIPice(false, epochSecondsOd, epochSecondsDo);
        return Response.ok(lista).build();
      } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    }

  /**
   * Dohvaća sve obračune unutar zadanog vremenskog raspona.
   * @param epochSecondsOd Početno vrijeme u sekundama od epohe.
   * @param epochSecondsDo Krajnje vrijeme u sekundama od epohe.
   * @return Lista obračuna unutar intervala.
   */
    @Path("obracun")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat obračuna po vremenskom rasponu")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista obračuna"),
            @APIResponse(responseCode = "400", description = "Neispravan format parametara"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    public Response getObracunOdDo(
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    ) {
      try (var vezaBP = restConfiguration.dajVezu()) {
        var dao = new PartnerDAO(vezaBP);
        List<Obracun> lista = dao.getObracuniOdDo(epochSecondsOd, epochSecondsDo);
        return Response.ok(lista).build();
      } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    }

  /**
   * Dohvaća obračune za određenog partnera i vremenski raspon.
   * @param id ID partnera.
   * @param epochSecondsOd Početno vrijeme u sekundama od epohe.
   * @param epochSecondsDo Krajnje vrijeme u sekundama od epohe.
   * @return Lista obračuna za partnera unutar intervala.
   */
    @Path("obracun/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dohvat obračuna po vremenskom rasponu")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista obračuna"),
            @APIResponse(responseCode = "400", description = "Neispravan format parametara"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    public Response getObracunOdDo(
            @PathParam("id") int id,
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    ) {
      try (var vezaBP = restConfiguration.dajVezu()) {
        var dao = new PartnerDAO(vezaBP);
        List<Obracun> lista = dao.getObracuniIdOdDo(id, epochSecondsOd, epochSecondsDo);
        return Response.ok(lista).build();
      } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    }
  /**
   * Pauzira poslužitelj tvrtke na zadano vrijeme.
   * @param trajanjeSpavanjaMilis Vrijeme spavanja u milisekundama.
   * @return HTTP 200 ako je uspješno, HTTP 409 ako konflikt.
   */
    @Path("spava")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Spavanje servera")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Server spava"),
            @APIResponse(responseCode = "400", description = "Neispravan format parametara"),
            @APIResponse(responseCode = "500", description = "Interna pogreška")
    })
    public Response getSpavanje(
            @QueryParam("vrijeme") int trajanjeSpavanjaMilis
    ) {
        String komanda = "SPAVA " + this.kodZaAdminTvrtke + " " + trajanjeSpavanjaMilis;
        String odgovor = posaljiKomandu(komanda, this.mreznaVrataKraj);
        if (odgovor != null || odgovor.contains("OK")) {
          return Response.status(Response.Status.OK).build();
        } else {
          return Response.status(Response.Status.CONFLICT).build();
        }
    }

  /**
   * Šalje komandu na virtualni poslužitelj.
   * @param komanda Naredba koja se šalje.
   * @param mreznaVrata Broj mrežnih vrata.
   * @return Odgovor poslužitelja ili null.
   */
    private String posaljiKomandu(String komanda, String mreznaVrata) {
      try (Socket mreznaUticnica = new Socket(this.tvrtkaAdresa,
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

  /**
   * Spaja više JSON nizova u jednu JsonArray.
   * @param jsonInputs Lista JSON stringova koje treba spojiti.
   * @return Kombinirani JsonElement (JsonArray) svih ispravnih ulaza.
   */
    private static JsonElement spojiSveJsonove(List<String> jsonInputs) {
      JsonArray combined = new JsonArray();
      for (String json : jsonInputs) {
        if (json == null || json.isBlank()) {
          continue;
        }
        try {
          JsonParser.parseString(json).getAsJsonArray().forEach(combined::add);
        } catch (JsonSyntaxException e) {
        }
      }
      return combined;
    }


  /**
   * Kreira interne objekte Partner iz JSON unosa.
   * @param ulaz JSON string koji predstavlja niz objekata Partner.
   */
    private void napraviObjektPartneri(String ulaz){
      Gson gson = new Gson();
      var partnerNiz = gson.fromJson(ulaz, Partner[].class);
      Arrays.stream(partnerNiz).forEach(pr -> this.partneri.put(pr.id(), pr));
    }
}


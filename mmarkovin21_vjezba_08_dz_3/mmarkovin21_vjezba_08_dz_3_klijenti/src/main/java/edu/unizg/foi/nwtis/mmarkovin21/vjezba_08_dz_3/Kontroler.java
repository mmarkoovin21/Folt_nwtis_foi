/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3;

import java.util.List;
import java.util.logging.Logger;

import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.View;
import jakarta.mvc.binding.BindingResult;
import jakarta.ws.rs.core.GenericType;

/**
 * @author NWTiS
 */
@Controller
@Path("tvrtka")
@RequestScoped
public class Kontroler {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Inject
    private Models model;

    @Inject
    private BindingResult bindingResult;

    @Inject
    @RestClient
    ServisTvrtkaKlijent servisTvrtka;

    @GET
    @Path("pocetak")
    @View("index.jsp")
    public void pocetak() {
    }

    @GET
    @Path("kraj")
    @View("status.jsp")
    public void kraj() {
        var status = this.servisTvrtka.headPosluziteljKraj().getStatus();
        this.model.put("statusOperacije", status);
        dohvatiStatuse();
    }

    @GET
    @Path("status")
    @View("status.jsp")
    public void status() {
        dohvatiStatuse();
    }

    @GET
    @Path("start/{id}")
    @View("status.jsp")
    public void startId(@PathParam("id") int id) {
        var status = this.servisTvrtka.headPosluziteljStart(id).getStatus();
        this.model.put("status", status);
        this.model.put("samoOperacija", true);
    }

    @GET
    @Path("pauza/{id}")
    @View("status.jsp")
    public void pauzatId(@PathParam("id") int id) {
        var status = this.servisTvrtka.headPosluziteljPauza(id).getStatus();
        this.model.put("status", status);
        this.model.put("samoOperacija", true);
    }

    @GET
    @Path("partner")
    @View("partneri.jsp")
    public void partneri() {
        var odgovor = this.servisTvrtka.getPartneri();
        var status = odgovor.getStatus();
        if (status == 200) {
            var partneri = odgovor.readEntity(new GenericType<List<Partner>>() {
            });
            this.model.put("status", status);
            this.model.put("partneri", partneri);
        }
    }

    @GET
    @Path("partner/dodaj")
    @View("noviPartner.jsp")
    public void prikaziFormuNoviPartner() {
    }

    @POST
    @Path("partner/dodaj")
    @View("noviPartner.jsp")
    public void dodajPartner(
            @FormParam("id")            int    id,
            @FormParam("naziv")           String naziv,
            @FormParam("vrstaKuhinje")    String vrstaKuhinje,
            @FormParam("adresa")          String adresa,
            @FormParam("mreznaVrata")     int    mreznaVrata,
            @FormParam("mreznaVrataKraj") int    mreznaVrataKraj,
            @FormParam("gpsSirina")       float  gpsSirina,
            @FormParam("gpsDuzina")       float  gpsDuzina,
            @FormParam("sigurnosniKod")   String sigurnosniKod,
            @FormParam("adminKod")        String adminKod
    ) {
        model.put("id", id);
        model.put("naziv", naziv);
        model.put("vrstaKuhinje", vrstaKuhinje);
        model.put("adresa", adresa);
        model.put("mreznaVrata", mreznaVrata);
        model.put("mreznaVrataKraj", mreznaVrataKraj);
        model.put("gpsSirina", gpsSirina);
        model.put("gpsDuzina", gpsDuzina);
        model.put("sigurnosniKod", sigurnosniKod);
        model.put("adminKod", adminKod);

        Partner novi = new Partner(
                id,
                naziv,
                vrstaKuhinje,
                adresa,
                mreznaVrata,
                mreznaVrataKraj,
                gpsSirina,
                gpsDuzina,
                sigurnosniKod,
                adminKod
        );  

        Response resp = servisTvrtka.postPartner(novi);
        logger.warning(resp.toString());
        if (resp.getStatus() == 201) {
            model.put("poruka", "Partner je uspješno dodan.");
            model.put("pogreska", false);
        } else {
            model.put("poruka", "Greška pri dodavanju partnera: status " + resp.getStatus());
            model.put("pogreska", true);
            logger.warning("Dodavanje partnera vratilo: " + resp.getStatus());
        }
    }

    @GET
    @Path("partner/{id}")
    @View("partnerDetalji.jsp")
    public void partneri(
            @PathParam("id") int id
    ) {
        var odgovor = this.servisTvrtka.getPartner(id);
        var status = odgovor.getStatus();
        if (status == 200) {
            var partner = odgovor.readEntity(new GenericType<Partner>() {
            });
            this.model.put("status", status);
            this.model.put("partner", partner);
        }
    }


    @GET
    @Path("privatno/obracun")
    @View("obracuni.jsp")
    public void listaObracuna(
            @QueryParam("od") Long odMillis,
            @QueryParam("do") Long doMillis
    ) {
        loadPartneri();
        model.put("od", odMillis);
        model.put("do", doMillis);

        var resp = servisTvrtka.getObracun(odMillis, doMillis);
        if (resp.getStatus() == 200) {
            var obr = resp.readEntity(new GenericType<List<Obracun>>() {
            });
            model.put("obracuni", obr);
        }
    }

    @GET
    @Path("privatno/obracun/jelo")
    @View("obracuni.jsp")
    public void dohvatiObracuneJelo(
            @QueryParam("od") Long odMillis,
            @QueryParam("do") Long doMillis
    ) {
        loadPartneri();
        model.put("od", odMillis);
        model.put("do", doMillis);

        var odgovor = servisTvrtka.getObracunJelo(odMillis, doMillis);
        if (odgovor.getStatus() == 200) {
            var obracuni = odgovor.readEntity(new GenericType<List<Obracun>>() {
            });
            model.put("obracuni", obracuni);
        }
    }

    @GET
    @Path("privatno/obracun/pice")
    @View("obracuni.jsp")
    public void dohvatiObracunePice(
            @QueryParam("od") Long odMillis,
            @QueryParam("do") Long doMillis
    ) {
        loadPartneri();
        model.put("od", odMillis);
        model.put("do", doMillis);

        var odgovor = servisTvrtka.getObracunPice(odMillis, doMillis);
        if (odgovor.getStatus() == 200) {
            var obracuni = odgovor.readEntity(new GenericType<List<Obracun>>() {
            });
            model.put("obracuni", obracuni);
        }
    }

    @GET
    @Path("privatno/obracun/{id}")
    @View("obracuni.jsp")
    public void dohvatiObracuneId(
            @PathParam("id") int id,
            @QueryParam("od") Long odDatum,
            @QueryParam("do") Long doDatum
    ) {
        loadPartneri();
        var odgovor = servisTvrtka.getObracunId(id, odDatum, doDatum);
        var status = odgovor.getStatus();
        if (status == 200) {
            var obracuni = odgovor.readEntity(new GenericType<List<Obracun>>() {
            });
            model.put("obracuni", obracuni);
        }
        model.put("od", odDatum);
        model.put("do", doDatum);
    }

    private void loadPartneri() {
        var resp = servisTvrtka.getPartneri();
        if (resp.getStatus() == 200) {
            var lista = resp.readEntity(new GenericType<List<Partner>>() {
            });
            model.put("partneri", lista);
        } else {
            model.put("partneri", List.<Partner>of());
        }
    }

    private void dohvatiStatuse() {
        this.model.put("samoOperacija", false);
        var statusT = this.servisTvrtka.headPosluzitelj().getStatus();
        this.model.put("statusT", statusT);
        var statusT1 = this.servisTvrtka.headPosluziteljStatus(1).getStatus();
        this.model.put("statusT1", statusT1);
        var statusT2 = this.servisTvrtka.headPosluziteljStatus(2).getStatus();
        this.model.put("statusT2", statusT2);
    }

    @GET
    @Path("admin/nadzornaKonzolaTvrtka")
    @View("nadzornaKonzolaTvrtka.jsp")
    public void nadzornaKonzolaTvrtka() {
    }
}

package edu.unizg.foi.nwtis.mmarkovin21.vjezba_08_dz_3;

import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RegisterRestClient(configKey = "klijentTvrtka")
@Path("api/tvrtka")
public interface ServisTvrtkaKlijent {
    @HEAD
    public Response headPosluzitelj();

    @Path("status/{id}")
    @HEAD
    public Response headPosluziteljStatus(@PathParam("id") int id);

    @Path("pauza/{id}")
    @HEAD
    public Response headPosluziteljPauza(@PathParam("id") int id);

    @Path("start/{id}")
    @HEAD
    public Response headPosluziteljStart(@PathParam("id") int id);

    @Path("kraj")
    @HEAD
    public Response headPosluziteljKraj();

    @Path("kraj/info")
    @HEAD
    public Response headPosluziteljKrajInfo();

    @Path("partner")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPartneri();

    @Path("partner/{id}")
    @GET
    public Response getPartner(@PathParam("id") int id);

    @Path("partner")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response postPartner(Partner partner);

    @Path("partner/provjera")
    @GET
    public Response getPartnerProvjera();

    @Path("kartapica")
    @GET
    public Response getKartapica();

    @Path("jelovnik")
    @GET
    public Response getJelovnik();

    @Path("jelovnik/{id}")
    @GET
    public Response getJelovnikId(@PathParam("id") int id);

    @Path("obracun")
    @POST
    public Response postObracun(List<Obracun> obracuni);

    @Path("obracun/ws")
    @POST
    public Response postObracunWs(List<Obracun> obracuni);

    @Path("obracun/{id}")
    @POST
    public Response getObracunOdDo(
            @PathParam("id") int id,
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    );

    @Path("obracun/jelo")
    @GET
    public Response getObracunJelo(
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    );

    @Path("obracun/pice")
    @GET
    public Response getObracunPice(
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    );
    @Path("obracun")
    @GET
    public Response getObracun(
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    );

    @Path("obracun/{id}")
    @GET
    public Response getObracunId(
            @PathParam("id") int id,
            @QueryParam("od") Long epochSecondsOd,
            @QueryParam("do") Long epochSecondsDo
    );

    @Path("spava")
    @GET
    public Response getSpava(
            @QueryParam("vrijeme") int trajanjeSpavanjaMilis
    );
}

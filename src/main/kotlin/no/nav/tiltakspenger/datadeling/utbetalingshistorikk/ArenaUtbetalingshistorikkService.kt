package no.nav.tiltakspenger.datadeling.utbetalingshistorikk

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.ArenaAnmerkningResponse
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.ArenaUtbetalingshistorikkDetaljerResponse
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.ArenaUtbetalingshistorikkResponse
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes.ArenaVedtakfaktaResponse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class ArenaUtbetalingshistorikkService(
    private val arenaClient: ArenaClient,
) {
    suspend fun hentUtbetalingshistorikk(fnr: Fnr, periode: Periode): List<ArenaUtbetalingshistorikkResponse> {
        return arenaClient.hentUtbetalingshistorikk(
            ArenaClient.ArenaRequestDTO(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        ).map {
            ArenaUtbetalingshistorikkResponse(
                meldekortId = it.meldekortId,
                dato = it.dato,
                transaksjonstype = it.transaksjonstype,
                sats = it.sats,
                status = it.status,
                vedtakId = it.vedtakId,
                belop = it.belop,
                fraOgMedDato = it.fraOgMedDato,
                tilOgMedDato = it.tilOgMedDato,
            )
        }
    }

    suspend fun hentUtbetalingshistorikkDetaljer(
        meldekortId: Long?,
        vedtakId: Long?,
    ): ArenaUtbetalingshistorikkDetaljerResponse {
        val detaljer = arenaClient.hentUtbetalingshistorikkDetaljer(
            ArenaClient.ArenaUtbetalingshistorikkDetaljerRequest(
                meldekortId = meldekortId,
                vedtakId = vedtakId,
            ),
        )
        return ArenaUtbetalingshistorikkDetaljerResponse(
            vedtakfakta = detaljer.vedtakfakta?.let { vedtakfakta ->
                ArenaVedtakfaktaResponse(
                    dagsats = vedtakfakta.dagsats,
                    gjelderFra = vedtakfakta.gjelderFra,
                    gjelderTil = vedtakfakta.gjelderTil,
                    antallUtbetalinger = vedtakfakta.antallUtbetalinger,
                    belopPerUtbetalinger = vedtakfakta.belopPerUtbetalinger,
                    alternativBetalingsmottaker = vedtakfakta.alternativBetalingsmottaker,
                )
            },
            anmerkninger = detaljer.anmerkninger.map { anmerkning ->
                ArenaAnmerkningResponse(
                    kilde = anmerkning.kilde,
                    registrert = anmerkning.registrert,
                    beskrivelse = anmerkning.beskrivelse,
                )
            },
        )
    }
}

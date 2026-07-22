package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes

import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer

data class ArenaUtbetalingshistorikkDetaljerResponse(
    val vedtakfakta: ArenaVedtakfaktaResponse?,
    val anmerkninger: List<ArenaAnmerkningResponse>,
)

internal fun ArenaUtbetalingshistorikkDetaljer.toArenaUtbetalingshistorikkDetaljerResponse(): ArenaUtbetalingshistorikkDetaljerResponse =
    ArenaUtbetalingshistorikkDetaljerResponse(
        vedtakfakta = vedtakfakta?.let { vedtakfakta ->
            ArenaVedtakfaktaResponse(
                dagsats = vedtakfakta.dagsats,
                gjelderFra = vedtakfakta.gjelderFra,
                gjelderTil = vedtakfakta.gjelderTil,
                antallUtbetalinger = vedtakfakta.antallUtbetalinger,
                belopPerUtbetalinger = vedtakfakta.belopPerUtbetalinger,
                alternativBetalingsmottaker = vedtakfakta.alternativBetalingsmottaker,
            )
        },
        anmerkninger = anmerkninger.map { anmerkning ->
            ArenaAnmerkningResponse(
                kilde = anmerkning.kilde,
                registrert = anmerkning.registrert,
                beskrivelse = anmerkning.beskrivelse,
            )
        },
    )

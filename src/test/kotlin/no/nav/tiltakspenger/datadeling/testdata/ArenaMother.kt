package no.nav.tiltakspenger.datadeling.testdata

import no.nav.tiltakspenger.datadeling.arena.ArenaAnmerkning
import no.nav.tiltakspenger.datadeling.arena.ArenaMeldekort
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikkDetaljer
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtakfakta
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Testdata for det Arena svarer med.
 * Verdiene er vilkårlige, men faste, slik at route-testene kan asserte på hele JSON-strengen.
 */
object ArenaMother {
    fun meldekort(
        meldekortId: String = "1234567",
        mottatt: LocalDate? = LocalDate.of(2024, 1, 15),
        ferie: Boolean? = false,
        dager: List<ArenaMeldekort.ArenaMeldekortDag> = listOf(meldekortdag()),
    ): ArenaMeldekort = ArenaMeldekort(
        meldekortId = meldekortId,
        mottatt = mottatt,
        arbeidet = true,
        kurs = false,
        ferie = ferie,
        syk = false,
        annetFravaer = false,
        fortsattArbeidsoker = true,
        registrert = LocalDateTime.parse("2024-01-15T10:00:00"),
        sistEndret = LocalDateTime.parse("2024-01-16T10:00:00"),
        type = "ELEKTRONISK",
        status = "FERDIG",
        statusDato = LocalDate.of(2024, 1, 16),
        meldegruppe = "INDIV",
        aar = 2024,
        totaltArbeidetTimer = 15,
        periode = ArenaMeldekort.ArenaMeldekortPeriode(
            aar = 2024,
            periodekode = 202401,
            ukenrUke1 = 1,
            ukenrUke2 = 2,
            fraOgMed = LocalDate.of(2024, 1, 1),
            tilOgMed = LocalDate.of(2024, 1, 14),
        ),
        dager = dager,
    )

    fun meldekortdag(
        ferie: Boolean? = false,
    ): ArenaMeldekort.ArenaMeldekortDag = ArenaMeldekort.ArenaMeldekortDag(
        ukeNr = 1,
        dagNr = 1,
        arbeidsdag = true,
        ferie = ferie,
        kurs = false,
        syk = false,
        annetFravaer = false,
        registrertAv = "BRUKER",
        registrert = LocalDateTime.parse("2024-01-15T10:00:00"),
        arbeidetTimer = 7,
    )

    fun utbetalingshistorikk(
        meldekortId: Long? = 1234567,
        vedtakId: Long? = 7654321,
    ): ArenaUtbetalingshistorikk = ArenaUtbetalingshistorikk(
        meldekortId = meldekortId,
        dato = LocalDate.of(2024, 1, 20),
        transaksjonstype = "UTBETALING",
        sats = 285.0,
        status = "UTBETALT",
        vedtakId = vedtakId,
        belop = 1995.0,
        fraOgMedDato = LocalDate.of(2024, 1, 1),
        tilOgMedDato = LocalDate.of(2024, 1, 14),
    )

    fun utbetalingshistorikkDetaljer(
        vedtakfakta: ArenaVedtakfakta? = vedtakfakta(),
        anmerkninger: List<ArenaAnmerkning> = listOf(anmerkning()),
    ): ArenaUtbetalingshistorikkDetaljer = ArenaUtbetalingshistorikkDetaljer(
        vedtakfakta = vedtakfakta,
        anmerkninger = anmerkninger,
    )

    fun vedtakfakta(): ArenaVedtakfakta = ArenaVedtakfakta(
        dagsats = 285,
        gjelderFra = LocalDate.of(2024, 1, 1),
        gjelderTil = LocalDate.of(2024, 6, 30),
        antallUtbetalinger = 13,
        belopPerUtbetalinger = 1995,
        alternativBetalingsmottaker = null,
    )

    fun anmerkning(): ArenaAnmerkning = ArenaAnmerkning(
        kilde = "ARENA",
        registrert = LocalDateTime.parse("2024-01-20T09:00:00"),
        beskrivelse = "Utbetaling gjennomført",
    )
}

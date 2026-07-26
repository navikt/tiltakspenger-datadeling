package no.nav.tiltakspenger.datadeling.behandling.infra

import kotliquery.Row
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Leser behandlingens periode fra en rad i `behandling`-tabellen.
 *
 * Behandlingen kan mangle periode når tilstanden er KLAR_TIL_BEHANDLING, UNDER_BEHANDLING eller AVBRUTT — da opprettes den uten periode, og saksbehandler velger den senere.
 * Halve perioder avvises av check-constraintet `behandling_fra_og_med_til_og_med_begge_eller_ingen` (V33); sjekken her fanger dem opp dersom kravet skulle falle bort.
 *
 * Delt mellom [BehandlingPostgresRepo] og [no.nav.tiltakspenger.datadeling.vedtak.infra.repo.HentSakPostgresRepo], som leser den samme tabellen.
 */
internal fun behandlingsperiodeFraRow(row: Row): Periode? {
    val fraOgMed = row.localDateOrNull("fra_og_med")
    val tilOgMed = row.localDateOrNull("til_og_med")
    return when {
        fraOgMed == null && tilOgMed == null -> null

        fraOgMed != null && tilOgMed != null -> Periode(fraOgMed, tilOgMed)

        else -> throw IllegalStateException(
            "Behandling ${row.string("behandling_id")} har ugyldig periode: " +
                "fra_og_med og til_og_med må enten begge være null eller begge ha verdi",
        )
    }
}

package no.nav.tiltakspenger.datadeling.behandling.db

import kotliquery.Row
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Mapper en rad fra behandling-tabellen til domeneobjektet [TiltakspengerBehandling].
 * Krever at raden inneholder kolonnene fra behandling-tabellen.
 */
internal fun behandlingFromRow(row: Row): TiltakspengerBehandling {
    val fraOgMed = row.localDateOrNull("fra_og_med")
    val tilOgMed = row.localDateOrNull("til_og_med")
    return TiltakspengerBehandling(
        periode = if (fraOgMed != null && tilOgMed != null) Periode(fraOgMed, tilOgMed) else null,
        behandlingId = row.string("behandling_id"),
        sakId = row.string("sak_id"),
        behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.valueOf(row.string("behandling_status")),
        saksbehandler = row.stringOrNull("saksbehandler"),
        beslutter = row.stringOrNull("beslutter"),
        iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
        opprettetTidspunktSaksbehandlingApi = row.localDateTime("opprettet_tidspunkt_saksbehandling_api"),
        mottattTidspunktDatadeling = row.localDateTime("mottatt_tidspunkt_datadeling"),
        behandlingstype = TiltakspengerBehandling.Behandlingstype.valueOf(row.string("behandlingstype")),
        sistEndret = row.localDateTime("sist_endret"),
    )
}

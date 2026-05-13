package no.nav.tiltakspenger.datadeling.vedtak.db

import kotliquery.Row
import no.nav.tiltakspenger.datadeling.felles.infra.db.PeriodeDbJson
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periode.Periode
import tools.jackson.module.kotlin.readValue

/**
 * Mapper en rad fra rammevedtak-tabellen til domeneobjektet [TiltakspengerVedtak].
 * Krever at raden minst inneholder kolonnene fra rammevedtak-tabellen.
 */
internal fun rammevedtakFromRow(row: Row): TiltakspengerVedtak {
    val periode = Periode(
        row.localDate("fra_og_med"),
        row.localDate("til_og_med"),
    )
    val rettighet = TiltakspengerVedtak.Rettighet.valueOf(row.string("rettighet"))
    val innvilgelsesperiode = row.stringOrNull("innvilgelsesperiode")
        ?.let { deserialize<PeriodeDbJson>(it).toDomain() }
    val virkningsperiode = Periode(
        fraOgMed = row.localDateOrNull("virkningsperiode_fra_og_med") ?: periode.fraOgMed,
        tilOgMed = row.localDateOrNull("virkningsperiode_til_og_med") ?: periode.tilOgMed,
    )
    return TiltakspengerVedtak(
        vedtakId = row.string("vedtak_id"),
        sakId = row.string("sak_id"),
        rettighet = rettighet,
        mottattTidspunkt = row.localDateTime("mottatt_tidspunkt"),
        opprettet = row.localDateTime("opprettet_tidspunkt"),
        barnetillegg = row.stringOrNull("barnetillegg")?.let { objectMapper.readValue<Barnetillegg>(it) },
        valgteHjemlerHarIkkeRettighet = row.stringOrNull("valgte_hjemler_har_ikke_rettighet")
            ?.let { objectMapper.readValue<List<TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet>>(it) },
        virkningsperiode = virkningsperiode,
        innvilgelsesperiode = when (rettighet) {
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
            TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            -> innvilgelsesperiode ?: virkningsperiode

            TiltakspengerVedtak.Rettighet.STANS,
            TiltakspengerVedtak.Rettighet.AVSLAG,
            TiltakspengerVedtak.Rettighet.OPPHØR,
            -> null
        },
        omgjørRammevedtakId = row.stringOrNull("omgjør_rammevedtak_id"),
        omgjortAvRammevedtakId = row.stringOrNull("omgjort_av_rammevedtak_id"),
    )
}

package no.nav.tiltakspenger.datadeling.vedtak.infra.repo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.infra.db.PeriodeDbJson
import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.HentSakRepo
import no.nav.tiltakspenger.datadeling.vedtak.SakForVedtakSak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import tools.jackson.module.kotlin.readValue

class HentSakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : HentSakRepo {
    override fun hentSakForVedtakSak(fnr: Fnr): SakForVedtakSak? {
        return sessionFactory.withSession { session ->
            val sak = session.run(
                queryOf(
                    "select * from sak where fnr = :fnr",
                    mapOf("fnr" to fnr.verdi),
                ).map { sakFromRow(it) }.asSingle,
            ) ?: return@withSession null

            sak.copy(
                rammevedtak = hentRammevedtakForSak(sak.id, session),
                behandlinger = hentBehandlingerForSak(sak.id, session),
            )
        }
    }

    private fun hentRammevedtakForSak(sakId: SakId, session: Session): List<TiltakspengerVedtak> =
        session.run(
            queryOf(
                """
                select r.*,
                       s.fnr as sak_fnr,
                       s.saksnummer as sak_saksnummer,
                       s.opprettet as sak_opprettet
                from rammevedtak r join sak s on s.id = r.sak_id
                where r.sak_id = :sak_id
                """.trimIndent(),
                mapOf("sak_id" to sakId.toString()),
            ).map { rammevedtakFromRow(it) }.asList,
        )

    private fun hentBehandlingerForSak(sakId: SakId, session: Session): List<TiltakspengerBehandling> =
        session.run(
            queryOf(
                """
                select b.*,
                       s.fnr as sak_fnr,
                       s.saksnummer as sak_saksnummer,
                       s.opprettet as sak_opprettet
                from behandling b join sak s on s.id = b.sak_id
                where b.sak_id = :sak_id
                """.trimIndent(),
                mapOf("sak_id" to sakId.toString()),
            ).map { behandlingFromRow(it) }.asList,
        )

    private fun sakFromRow(row: Row): SakForVedtakSak =
        SakForVedtakSak(
            id = SakId.fromString(row.string("id")),
            fnr = Fnr.fromString(row.string("fnr")),
            saksnummer = Saksnummer(row.string("saksnummer")),
            opprettet = row.localDateTime("opprettet"),
            rammevedtak = emptyList(),
            behandlinger = emptyList(),
        )

    private fun rammevedtakFromRow(row: Row): TiltakspengerVedtak {
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
            sakId = SakId.fromString(row.string("sak_id")),
            saksnummer = Saksnummer(row.string("sak_saksnummer")),
            fnr = Fnr.fromString(row.string("sak_fnr")),
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

    private fun behandlingFromRow(row: Row): TiltakspengerBehandling {
        val fraOgMed = row.localDateOrNull("fra_og_med")
        val tilOgMed = row.localDateOrNull("til_og_med")
        val periode = when {
            fraOgMed == null && tilOgMed == null -> null

            fraOgMed != null && tilOgMed != null -> Periode(fraOgMed, tilOgMed)

            else -> throw IllegalStateException(
                "Behandling ${row.string("behandling_id")} har ugyldig periode: " +
                    "fra_og_med og til_og_med må enten begge være null eller begge ha verdi",
            )
        }
        return TiltakspengerBehandling(
            periode = periode,
            behandlingId = row.string("behandling_id"),
            sakId = SakId.fromString(row.string("sak_id")),
            saksnummer = Saksnummer(row.string("sak_saksnummer")),
            fnr = Fnr.fromString(row.string("sak_fnr")),
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
}

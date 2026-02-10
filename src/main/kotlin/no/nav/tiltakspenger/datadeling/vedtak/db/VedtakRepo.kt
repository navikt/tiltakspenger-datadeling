package no.nav.tiltakspenger.datadeling.vedtak.db

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.application.db.toPGObject
import no.nav.tiltakspenger.datadeling.felles.infra.db.PeriodeDbJson
import no.nav.tiltakspenger.datadeling.felles.infra.db.toDbJson
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime

class VedtakRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    val log = KotlinLogging.logger { }

    fun lagre(vedtak: TiltakspengerVedtak) {
        sessionFactory.withTransaction { session ->
            log.info { "Sletter eksisterende vedtak med id ${vedtak.vedtakId} hvis den finnes" }
            slettEksisterende(vedtak.vedtakId, session).also {
                log.info { "Slettet $it rader. Lagrer vedtak med id ${vedtak.vedtakId}" }
            }
            session.run(
                queryOf(
                    """
                    insert into rammevedtak (
                      vedtak_id,
                      sak_id,
                      fra_og_med,
                      til_og_med,
                      rettighet,
                      opprettet_tidspunkt,
                      mottatt_tidspunkt,
                      barnetillegg,
                      valgte_hjemler_har_ikke_rettighet,
                      sendt_til_obo,
                      virkningsperiode_fra_og_med,
                      virkningsperiode_til_og_med,
                      innvilgelsesperiode,
                      omgjør_rammevedtak_id,
                      omgjort_av_rammevedtak_id
                    ) values (
                      :vedtak_id,
                      :sak_id,
                      :fra_og_med,
                      :til_og_med,
                      :rettighet,
                      :opprettet_tidspunkt,
                      :mottatt_tidspunkt,
                      :barnetillegg,
                      :valgte_hjemler_har_ikke_rettighet,
                      :sendt_til_obo,
                      :virkningsperiode_fra_og_med,
                      :virkningsperiode_til_og_med,
                      :innvilgelsesperiode,
                      :omgjor_rammevedtak_id,
                      :omgjort_av_rammevedtak_id
                    )
                    """.trimIndent(),
                    mapOf(
                        "vedtak_id" to vedtak.vedtakId,
                        "sak_id" to vedtak.sakId,
                        "fra_og_med" to vedtak.virkningsperiode.fraOgMed,
                        "til_og_med" to vedtak.virkningsperiode.tilOgMed,
                        "rettighet" to vedtak.rettighet.name,
                        "opprettet_tidspunkt" to vedtak.opprettet,
                        "mottatt_tidspunkt" to vedtak.mottattTidspunkt,
                        "barnetillegg" to toPGObject(vedtak.barnetillegg),
                        "valgte_hjemler_har_ikke_rettighet" to toPGObject(vedtak.valgteHjemlerHarIkkeRettighet),
                        "sendt_til_obo" to null,
                        "virkningsperiode_fra_og_med" to vedtak.virkningsperiode.fraOgMed,
                        "virkningsperiode_til_og_med" to vedtak.virkningsperiode.tilOgMed,
                        "innvilgelsesperiode" to vedtak.innvilgelsesperiode?.let { toPGObject(it.toDbJson()) },
                        "omgjor_rammevedtak_id" to vedtak.omgjørRammevedtakId,
                        "omgjort_av_rammevedtak_id" to vedtak.omgjortAvRammevedtakId,
                    ),
                ).asUpdate,
            )
        }
        log.info { "Vedtak med id ${vedtak.vedtakId} lagret." }
    }

    private fun slettEksisterende(
        vedtakId: String,
        session: Session,
    ): Int {
        return session.run(
            queryOf(
                "delete from rammevedtak where vedtak_id = :vedtak_id",
                mapOf(
                    "vedtak_id" to vedtakId,
                ),
            ).asUpdate,
        )
    }

    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
    ): List<TiltakspengeVedtakMedSak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select r.*,
                       s.fnr as sak_fnr,
                       s.saksnummer as sak_saksnummer,
                       s.opprettet as sak_opprettet
                    from rammevedtak r join sak s on s.id = r.sak_id
                    where s.fnr = :fnr 
                    and fra_og_med <= :tilOgMed 
                    and til_og_med >= :fraOgMed
                    """.trimIndent(),
                    mapOf(
                        "fraOgMed" to periode.fraOgMed,
                        "tilOgMed" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    fromRow(it)
                }.asList,
            )
        }
    }

    fun hentForFnr(
        fnr: Fnr,
    ): List<TiltakspengeVedtakMedSak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select r.*,
                       s.fnr as sak_fnr,
                       s.saksnummer as sak_saksnummer,
                       s.opprettet as sak_opprettet
                    from rammevedtak r join sak s on s.id = r.sak_id
                    where s.fnr = :fnr
                    """.trimIndent(),
                    mapOf(
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    fromRow(it)
                }.asList,
            )
        }
    }

    fun hentForVedtakId(
        vedtakId: String,
        session: Session,
    ): TiltakspengeVedtakMedSak? {
        return session.run(
            queryOf(
                """
                    select r.*,
                       s.fnr as sak_fnr,
                       s.saksnummer as sak_saksnummer,
                       s.opprettet as sak_opprettet
                    from rammevedtak r join sak s on s.id = r.sak_id
                    where vedtak_id = :vedtak_id
                """.trimIndent(),
                mapOf(
                    "vedtak_id" to vedtakId,
                ),
            ).map {
                fromRow(it)
            }.asSingle,
        )
    }

    fun hentRammevedtakSomSkalDelesMedObo(
        limit: Int = 20,
    ): List<TiltakspengeVedtakMedSak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select r.*,
                       s.fnr as sak_fnr,
                       s.saksnummer as sak_saksnummer,
                       s.opprettet as sak_opprettet
                    from rammevedtak r join sak s on s.id = r.sak_id
                    where sendt_til_obo is null
                    and rettighet != 'AVSLAG'
                    order by r.opprettet_tidspunkt
                    limit $limit
                    """.trimIndent(),
                ).map {
                    fromRow(it)
                }.asList,
            )
        }
    }

    fun markerSendtTilObo(
        vedtakId: String,
        tidspunkt: LocalDateTime,
    ) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """update rammevedtak set sendt_til_obo = :tidspunkt where vedtak_id = :vedtak_id""",
                    mapOf(
                        "tidspunkt" to tidspunkt,
                        "vedtak_id" to vedtakId,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun fromRow(row: Row): TiltakspengeVedtakMedSak {
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
        return TiltakspengeVedtakMedSak(
            sak = Sak(
                id = row.string("sak_id"),
                fnr = Fnr.fromString(row.string("sak_fnr")),
                saksnummer = row.string("sak_saksnummer"),
                opprettet = row.localDateTime("sak_opprettet"),
            ),
            vedtak = TiltakspengerVedtak(
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
                    TiltakspengerVedtak.Rettighet.TILTAKSPENGER, TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG ->
                        innvilgelsesperiode
                            ?: virkningsperiode

                    TiltakspengerVedtak.Rettighet.STANS, TiltakspengerVedtak.Rettighet.AVSLAG, TiltakspengerVedtak.Rettighet.OPPHØR -> null
                },
                omgjørRammevedtakId = row.stringOrNull("omgjør_rammevedtak_id"),
                omgjortAvRammevedtakId = row.stringOrNull("omgjort_av_rammevedtak_id"),
            ),
        )
    }
}

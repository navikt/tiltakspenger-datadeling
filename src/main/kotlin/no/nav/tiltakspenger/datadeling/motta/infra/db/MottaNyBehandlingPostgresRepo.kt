package no.nav.tiltakspenger.datadeling.motta.infra.db

import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.motta.app.MottaNyBehandlingRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

internal class MottaNyBehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MottaNyBehandlingRepo {
    val log = KotlinLogging.logger { }

    override fun lagre(behandling: TiltakspengerBehandling) {
        return sessionFactory.withTransaction { session ->
            log.info { "Sletter eksisterende behandling med id ${behandling.behandlingId} hvis den finnes" }
            slettEksisterende(behandling.behandlingId, session).also {
                log.info { "Slettet $it rader. Lagrer behandling med id ${behandling.behandlingId}" }
            }
            opprettBehandling(behandling, session)
            log.info { "Behandling med id ${behandling.behandlingId} lagret." }
        }
    }

    private fun opprettBehandling(
        behandling: TiltakspengerBehandling,
        session: Session,
    ) {
        session.run(
            queryOf(
                """
                    insert into behandling (
                      behandling_id,
                      fnr,
                      sak_id,
                      saksnummer,
                      søknad_journalpost_id,
                      fra_og_med,
                      til_og_med,
                      behandling_status,
                      saksbehandler,
                      beslutter,
                      tiltaksdeltagelse,
                      iverksatt_tidspunkt,
                      opprettet_tidspunkt_saksbehandling_api,
                      mottatt_tidspunkt_datadeling,
                      kilde
                    ) values (
                      :behandling_id,
                      :fnr,
                      :sak_id,
                      :saksnummer,
                      :soknad_journalpost_id,
                      :fra_og_med,
                      :til_og_med,
                      :behandling_status,
                      :saksbehandler,
                      :beslutter,
                      to_jsonb(:tiltaksdeltagelse::json),
                      :iverksatt_tidspunkt,
                      :opprettet_tidspunkt_saksbehandling_api,
                      :mottatt_tidspunkt_datadeling,
                      :kilde
                    )
                """.trimIndent(),
                mapOf(
                    "behandling_id" to behandling.behandlingId,
                    "fnr" to behandling.fnr.verdi,
                    "sak_id" to behandling.sakId,
                    "saksnummer" to behandling.saksnummer,
                    "soknad_journalpost_id" to behandling.søknadJournalpostId,
                    "fra_og_med" to behandling.periode.fraOgMed,
                    "til_og_med" to behandling.periode.tilOgMed,
                    "behandling_status" to behandling.behandlingStatus.name,
                    "saksbehandler" to behandling.saksbehandler,
                    "beslutter" to behandling.beslutter,
                    "tiltaksdeltagelse" to behandling.tiltaksdeltagelse.toDbJson(),
                    "iverksatt_tidspunkt" to behandling.iverksattTidspunkt,
                    "opprettet_tidspunkt_saksbehandling_api" to behandling.opprettetTidspunktSaksbehandlingApi,
                    "mottatt_tidspunkt_datadeling" to behandling.mottattTidspunktDatadeling,
                    "kilde" to behandling.kilde,
                ),
            ).asUpdate,
        )
    }

    private fun slettEksisterende(
        behandlingId: String,
        session: Session,
    ): Int {
        return session.run(
            queryOf(
                "delete from behandling where behandling_id = :behandling_id",
                mapOf(
                    "behandling_id" to behandlingId,
                ),
            ).asUpdate,
        )
    }

    fun hentForFnr(
        fnr: Fnr,
    ): TiltakspengerBehandling? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select * from behandling where fnr = :fnr",
                    mapOf(
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    TiltakspengerBehandling(
                        sakId = it.string("sak_id"),
                        saksnummer = it.string("saksnummer"),
                        fnr = Fnr.fromString(it.string("fnr")),
                        periode = Periode(
                            it.localDate("fra_og_med"),
                            it.localDate("til_og_med"),
                        ),
                        behandlingId = it.string("behandling_id"),
                        behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.valueOf(it.string("behandling_status")),
                        saksbehandler = it.string("saksbehandler"),
                        beslutter = it.string("beslutter"),
                        iverksattTidspunkt = it.localDateTime("iverksatt_tidspunkt"),
                        tiltaksdeltagelse = it.string("tiltaksdeltagelse").toTiltaksdeltagelse(),
                        søknadJournalpostId = it.string("søknad_journalpost_id"),
                        opprettetTidspunktSaksbehandlingApi = it.localDateTime("opprettet_tidspunkt_saksbehandling_api"),
                        mottattTidspunktDatadeling = it.localDateTime("mottatt_tidspunkt_datadeling"),
                    )
                }.asSingle,
            )
        }
    }
}

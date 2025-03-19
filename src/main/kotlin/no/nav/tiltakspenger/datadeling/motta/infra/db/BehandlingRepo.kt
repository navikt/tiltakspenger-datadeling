package no.nav.tiltakspenger.datadeling.motta.infra.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

class BehandlingRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    val log = KotlinLogging.logger { }

    fun lagre(behandling: TiltakspengerBehandling) {
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
                    "iverksatt_tidspunkt" to behandling.iverksattTidspunkt,
                    "opprettet_tidspunkt_saksbehandling_api" to behandling.opprettetTidspunktSaksbehandlingApi,
                    "mottatt_tidspunkt_datadeling" to behandling.mottattTidspunktDatadeling,
                    "kilde" to behandling.kilde.navn,
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

    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
        kilde: Kilde,
    ): List<TiltakspengerBehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select * from behandling 
                      where fnr = :fnr 
                      and kilde = :kilde
                      and fra_og_med <= :tilOgMed 
                      and til_og_med >= :fraOgMed
                    """.trimIndent(),
                    mapOf(
                        "fraOgMed" to periode.fraOgMed,
                        "tilOgMed" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                        // TODO post-mvp jah: Mangler kilde i databaseindeksen
                        "kilde" to kilde.navn,
                    ),
                ).map {
                    val kildeFraDatabase = it.string("kilde")
                    require(kildeFraDatabase == kilde.navn) { "Forventet kilde ${kilde.navn}, men var $kildeFraDatabase" }
                    fromRow(it)
                }.asList,
            )
        }
    }

    internal fun hentForFnr(
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
                    fromRow(it)
                }.asSingle,
            )
        }
    }
    private fun fromRow(row: Row): TiltakspengerBehandling = TiltakspengerBehandling(
        sakId = row.string("sak_id"),
        saksnummer = row.string("saksnummer"),
        fnr = Fnr.fromString(row.string("fnr")),
        periode = Periode(
            row.localDate("fra_og_med"),
            row.localDate("til_og_med"),
        ),
        behandlingId = row.string("behandling_id"),
        behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.valueOf(row.string("behandling_status")),
        saksbehandler = row.stringOrNull("saksbehandler"),
        beslutter = row.stringOrNull("beslutter"),
        iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
        søknadJournalpostId = row.string("søknad_journalpost_id"),
        opprettetTidspunktSaksbehandlingApi = row.localDateTime("opprettet_tidspunkt_saksbehandling_api"),
        mottattTidspunktDatadeling = row.localDateTime("mottatt_tidspunkt_datadeling"),
    )
}

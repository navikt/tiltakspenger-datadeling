package no.nav.tiltakspenger.datadeling.behandling.db

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
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

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """update behandling set fnr = :nytt_fnr where fnr = :gammelt_fnr""",
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
            )
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
                      fra_og_med,
                      til_og_med,
                      behandling_status,
                      saksbehandler,
                      beslutter,
                      iverksatt_tidspunkt,
                      opprettet_tidspunkt_saksbehandling_api,
                      mottatt_tidspunkt_datadeling,
                      behandlingstype,
                      sist_endret
                    ) values (
                      :behandling_id,
                      :fnr,
                      :sak_id,
                      :saksnummer,
                      :fra_og_med,
                      :til_og_med,
                      :behandling_status,
                      :saksbehandler,
                      :beslutter,
                      :iverksatt_tidspunkt,
                      :opprettet_tidspunkt_saksbehandling_api,
                      :mottatt_tidspunkt_datadeling,
                      :behandlingstype,
                      :sist_endret
                    )
                """.trimIndent(),
                mapOf(
                    "behandling_id" to behandling.behandlingId,
                    "fnr" to behandling.fnr.verdi,
                    "sak_id" to behandling.sakId,
                    "saksnummer" to behandling.saksnummer,
                    "fra_og_med" to behandling.periode?.fraOgMed,
                    "til_og_med" to behandling.periode?.tilOgMed,
                    "behandling_status" to behandling.behandlingStatus.name,
                    "saksbehandler" to behandling.saksbehandler,
                    "beslutter" to behandling.beslutter,
                    "iverksatt_tidspunkt" to behandling.iverksattTidspunkt,
                    "opprettet_tidspunkt_saksbehandling_api" to behandling.opprettetTidspunktSaksbehandlingApi,
                    "mottatt_tidspunkt_datadeling" to behandling.mottattTidspunktDatadeling,
                    "behandlingstype" to behandling.behandlingstype.name,
                    "sist_endret" to behandling.sistEndret,
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
    ): List<TiltakspengerBehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select * from behandling 
                      where fnr = :fnr
                      and (fra_og_med is not null and fra_og_med <= :tilOgMed) 
                      and (til_og_med is not null and til_og_med >= :fraOgMed)
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

    private fun fromRow(row: Row): TiltakspengerBehandling {
        val fraOgMed = row.localDateOrNull("fra_og_med")
        val tilOgMed = row.localDateOrNull("til_og_med")
        return TiltakspengerBehandling(
            sakId = row.string("sak_id"),
            saksnummer = row.string("saksnummer"),
            fnr = Fnr.Companion.fromString(row.string("fnr")),
            periode = if (fraOgMed != null && tilOgMed != null) {
                Periode(fraOgMed, tilOgMed)
            } else {
                null
            },
            behandlingId = row.string("behandling_id"),
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

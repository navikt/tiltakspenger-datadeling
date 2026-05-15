package no.nav.tiltakspenger.datadeling.behandling.db

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengeBehandlingMedSak
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.behandling.domene.apneBehandlingsstatuser
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

interface BehandlingRepo {
    fun lagre(behandling: TiltakspengerBehandling)

    /** Filtrerer bort behandlinger uten periode*/
    fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengerBehandling>
    fun hentApneBehandlinger(fnr: Fnr): List<TiltakspengeBehandlingMedSak>
    fun hentForFnr(fnr: Fnr): List<TiltakspengeBehandlingMedSak>
}

class PostgresBehandlingRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BehandlingRepo {
    val log = KotlinLogging.logger { }

    override fun lagre(behandling: TiltakspengerBehandling) {
        return sessionFactory.withTransaction { session ->
            log.info { "Sletter eksisterende behandling med id ${behandling.behandlingId} hvis den finnes" }
            slettEksisterende(behandling.behandlingId, session).also {
                log.info { "Slettet $it rader. Lagrer behandling med id ${behandling.behandlingId}" }
            }
            opprettBehandling(
                behandling = behandling,
                session,
            )
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
                      sak_id,
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
                      :sak_id,
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
                    "sak_id" to behandling.sakId.toString(),
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

    override fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
    ): List<TiltakspengerBehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                        select b.*,
                               s.fnr as sak_fnr,
                               s.saksnummer as sak_saksnummer,
                               s.opprettet as sak_opprettet
                        from behandling b join sak s on s.id = b.sak_id
                        where s.fnr = :fnr
                            and (fra_og_med is not null and fra_og_med <= :tilOgMed) 
                            and (til_og_med is not null and til_og_med >= :fraOgMed)
                    """.trimIndent(),
                    mapOf(
                        "fraOgMed" to periode.fraOgMed,
                        "tilOgMed" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    behandlingFromRow(it)
                }.asList,
            )
        }
    }

    override fun hentApneBehandlinger(
        fnr: Fnr,
    ): List<TiltakspengeBehandlingMedSak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select b.*,
                           s.fnr as sak_fnr,
                           s.saksnummer as sak_saksnummer,
                           s.opprettet as sak_opprettet
                    from behandling b join sak s on s.id = b.sak_id
                    where s.fnr = :fnr
                      and behandling_status = any(:apne_statuser)
                    """.trimIndent(),
                    mapOf(
                        "fnr" to fnr.verdi,
                        "apne_statuser" to apneBehandlingsstatuser.map { it.name }.toTypedArray(),
                    ),
                ).map {
                    fromRow(it)
                }.asList,
            )
        }
    }

    override fun hentForFnr(
        fnr: Fnr,
    ): List<TiltakspengeBehandlingMedSak> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select b.*,
                           s.fnr as sak_fnr,
                           s.saksnummer as sak_saksnummer,
                           s.opprettet as sak_opprettet
                    from behandling b join sak s on s.id = b.sak_id
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

    private fun fromRow(row: Row): TiltakspengeBehandlingMedSak {
        return TiltakspengeBehandlingMedSak(
            sak = sakFromRow(row),
            behandling = behandlingFromRow(row),
        )
    }

    private fun sakFromRow(row: Row): Sak {
        return Sak(
            id = SakId.fromString(row.string("sak_id")),
            fnr = Fnr.fromString(row.string("sak_fnr")),
            saksnummer = Saksnummer(row.string("sak_saksnummer")),
            opprettet = row.localDateTime("sak_opprettet"),
        )
    }

    private fun behandlingFromRow(row: Row): TiltakspengerBehandling {
        // Behandlingen vil kunne mangle periode når tilstanden er KLAR_TIL_BEHANDLING, UNDER_BEHANDLING og AVBRUTT (dette gjelder for behandlingene som opprettes uten en periode, men den velges av saksbehandler senere).
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

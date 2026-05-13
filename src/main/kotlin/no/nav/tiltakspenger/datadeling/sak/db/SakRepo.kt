package no.nav.tiltakspenger.datadeling.sak.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.behandling.db.behandlingFromRow
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.datadeling.vedtak.db.rammevedtakFromRow
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

interface SakRepo {
    fun lagre(sak: Sak)
    fun hentForFnr(fnr: Fnr): Sak?
    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr)
}

class PostgresSakRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SakRepo {
    override fun lagre(sak: Sak) {
        sessionFactory.withTransaction { session ->
            session.run(
                queryOf(
                    """
                    insert into sak (
                      id,
                      fnr,
                      saksnummer,
                      opprettet
                    ) values (
                      :id,
                      :fnr,
                      :saksnummer,
                      :opprettet
                    ) on conflict (id) do update set
                        fnr = :fnr,
                        saksnummer = :saksnummer,
                        opprettet = :opprettet
                    """.trimIndent(),
                    mapOf(
                        "id" to sak.id,
                        "fnr" to sak.fnr.verdi,
                        "saksnummer" to sak.saksnummer,
                        "opprettet" to sak.opprettet,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentForFnr(
        fnr: Fnr,
    ): Sak? {
        return sessionFactory.withSession { session ->
            val sak = session.run(
                queryOf(
                    """
                    select *
                    from sak s
                    where s.fnr = :fnr
                      and (
                        exists (select 1 from behandling b where b.sak_id = s.id)
                        or exists (select 1 from rammevedtak r where r.sak_id = s.id)
                      )
                    """.trimIndent(),
                    mapOf(
                        "fnr" to fnr.verdi,
                    ),
                ).map { sakFromRow(it) }.asSingle,
            ) ?: return@withSession null

            sak.copy(
                rammevedtak = hentRammevedtakForSak(sak.id, session),
                behandlinger = hentBehandlingerForSak(sak.id, session),
            )
        }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """update sak set fnr = :nytt_fnr where fnr = :gammelt_fnr""",
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun hentRammevedtakForSak(sakId: String, session: Session): List<TiltakspengerVedtak> =
        session.run(
            queryOf(
                "select * from rammevedtak where sak_id = :sak_id",
                mapOf("sak_id" to sakId),
            ).map { rammevedtakFromRow(it) }.asList,
        )

    private fun hentBehandlingerForSak(sakId: String, session: Session): List<TiltakspengerBehandling> =
        session.run(
            queryOf(
                "select * from behandling where sak_id = :sak_id",
                mapOf("sak_id" to sakId),
            ).map { behandlingFromRow(it) }.asList,
        )

    private fun sakFromRow(row: Row): Sak =
        Sak(
            id = row.string("id"),
            fnr = Fnr.fromString(row.string("fnr")),
            saksnummer = row.string("saksnummer"),
            opprettet = row.localDateTime("opprettet"),
        )
}

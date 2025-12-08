package no.nav.tiltakspenger.datadeling.sak.db

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.sak.domene.Sak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory

class SakRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun lagre(sak: Sak) {
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

    fun hentForFnr(
        fnr: Fnr,
    ): Sak? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select * from sak where fnr = :fnr",
                    mapOf(
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    fromRow(it)
                }.asSingle,
            )
        }
    }

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
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

    private fun fromRow(row: Row): Sak =
        Sak(
            id = row.string("id"),
            fnr = Fnr.fromString(row.string("fnr")),
            saksnummer = row.string("saksnummer"),
            opprettet = row.localDateTime("opprettet"),
        )
}

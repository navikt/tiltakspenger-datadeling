package no.nav.tiltakspenger.datadeling.meldekort.db

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.application.db.toPGObject
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import java.time.LocalDate

class MeldeperiodeRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    val log = KotlinLogging.logger { }

    fun lagre(meldeperioder: List<Meldeperiode>) {
        return sessionFactory.withTransaction { session ->
            meldeperioder.filterNot { it.minstEnDagGirRettIPerioden }
                .forEach {
                    log.info { "Sletter meldeperiode der ingen dager gir rett: ${it.id}" }
                    slett(it.id, session)
                }
            meldeperioder.filter { it.minstEnDagGirRettIPerioden }
                .forEach {
                    lagre(it, session)
                    log.info { "Lagret meldeperiode med id ${it.id}" }
                }
        }
    }

    private fun slett(
        id: MeldeperiodeId,
        session: Session,
    ): Int {
        return session.run(
            queryOf(
                "delete from meldeperiode where id = :id",
                mapOf(
                    "id" to id.toString(),
                ),
            ).asUpdate,
        )
    }

    private fun lagre(
        meldeperiode: Meldeperiode,
        session: Session,
    ) {
        session.run(
            sqlQuery(
                """
                    insert into meldeperiode (
                        id,
                        kjede_id,
                        sak_id,
                        saksnummer,
                        fnr,
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        maks_antall_dager_for_periode,
                        gir_rett
                    ) values (
                        :id,
                        :kjede_id,
                        :sak_id,
                        :saksnummer,
                        :fnr,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        :maks_antall_dager_for_periode,
                        :gir_rett
                    )
                    on conflict (id) do update set
                        kjede_id = :kjede_id,
                        sak_id = :sak_id,
                        saksnummer = :saksnummer,
                        fnr = :fnr,
                        opprettet = :opprettet,
                        fra_og_med = :fra_og_med,
                        til_og_med = :til_og_med,
                        maks_antall_dager_for_periode = :maks_antall_dager_for_periode,
                        gir_rett = :gir_rett
                """,
                "id" to meldeperiode.id.toString(),
                "kjede_id" to meldeperiode.kjedeId,
                "sak_id" to meldeperiode.sakId.toString(),
                "saksnummer" to meldeperiode.saksnummer,
                "fnr" to meldeperiode.fnr.verdi,
                "opprettet" to meldeperiode.opprettet,
                "fra_og_med" to meldeperiode.fraOgMed,
                "til_og_med" to meldeperiode.tilOgMed,
                "maks_antall_dager_for_periode" to meldeperiode.maksAntallDagerForPeriode,
                "gir_rett" to toPGObject(meldeperiode.girRett),
            ).asUpdate,
        )
    }

    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
    ): List<Meldeperiode> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select * from meldeperiode
                      where fnr = :fnr
                      and fra_og_med <= :til_og_med
                      and til_og_med >= :fra_og_med
                    """.trimIndent(),
                    mapOf(
                        "fra_og_med" to periode.fraOgMed,
                        "til_og_med" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    fromRow(it)
                }.asList,
            )
        }
    }

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """update meldeperiode set fnr = :nytt_fnr where fnr = :gammelt_fnr""",
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun fromRow(row: Row): Meldeperiode = Meldeperiode(
        id = MeldeperiodeId.fromString(row.string("id")),
        kjedeId = row.string("kjede_id"),
        fnr = Fnr.fromString(row.string("fnr")),
        sakId = SakId.fromString(row.string("sak_id")),
        saksnummer = row.string("saksnummer"),
        opprettet = row.localDateTime("opprettet"),
        fraOgMed = row.localDate("fra_og_med"),
        tilOgMed = row.localDate("til_og_med"),
        maksAntallDagerForPeriode = row.int("maks_antall_dager_for_periode"),
        girRett = objectMapper.readValue<Map<LocalDate, Boolean>>(row.string("gir_rett")),
    )
}

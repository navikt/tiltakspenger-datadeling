package no.nav.tiltakspenger.datadeling.meldekort.db

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.application.db.prefixColumn
import no.nav.tiltakspenger.datadeling.application.db.toPGObject
import no.nav.tiltakspenger.datadeling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.datadeling.meldekort.domene.MeldeperiodeOgGodkjentMeldekort
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
            meldeperioder.forEach {
                log.info { "Sletter meldeperiode: sakId: ${it.sakId}, kjedeId: ${it.kjedeId}, id: ${it.id}" }
                slett(it.sakId, it.kjedeId, session)
            }
            meldeperioder.filter { it.minstEnDagGirRettIPerioden }
                .forEach {
                    lagre(it, session)
                    log.info { "Lagret meldeperiode med id ${it.id}" }
                }
        }
    }

    private fun slett(
        sakId: SakId,
        kjedeId: String,
        session: Session,
    ): Int {
        return session.run(
            queryOf(
                "delete from meldeperiode where sak_id = :sak_id and kjede_id = :kjede_id",
                mapOf(
                    "sak_id" to sakId.toString(),
                    "kjede_id" to kjedeId,
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
                    meldeperiodeFromRow(it)
                }.asList,
            )
        }
    }

    fun hentMeldeperioderOgGodkjenteMeldekort(
        fnr: Fnr,
        periode: Periode,
    ): List<MeldeperiodeOgGodkjentMeldekort> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select m.id as "m.id",
                        m.kjede_id as "m.kjede_id",
                        m.sak_id as "m.sak_id",
                        m.saksnummer as "m.saksnummer",
                        m.fnr as "m.fnr",
                        m.opprettet as "m.opprettet",
                        m.fra_og_med as "m.fra_og_med",
                        m.til_og_med as "m.til_og_med",
                        m.maks_antall_dager_for_periode as "m.maks_antall_dager_for_periode",
                        m.gir_rett as "m.gir_rett",
                        gm.kjede_id as "gm.kjede_id",
                        gm.sak_id as "gm.sak_id",
                        gm.meldeperiode_id as "gm.meldeperiode_id",
                        gm.fnr as "gm.fnr",
                        gm.saksnummer as "gm.saksnummer",
                        gm.mottatt_tidspunkt as "gm.mottatt_tidspunkt",
                        gm.vedtatt_tidspunkt as "gm.vedtatt_tidspunkt",
                        gm.behandlet_automatisk as "gm.behandlet_automatisk",
                        gm.korrigert as "gm.korrigert",
                        gm.fra_og_med as "gm.fra_og_med",
                        gm.til_og_med as "gm.til_og_med",
                        gm.meldekortdager as "gm.meldekortdager",
                        gm.opprettet as "gm.opprettet",
                        gm.sist_endret as "gm.sist_endret"
                    from meldeperiode m
                      left join godkjent_meldekort gm on m.sak_id = gm.sak_id and m.kjede_id = gm.kjede_id
                      where m.fnr = :fnr
                      and m.fra_og_med <= :til_og_med
                      and m.til_og_med >= :fra_og_med
                    """.trimIndent(),
                    mapOf(
                        "fra_og_med" to periode.fraOgMed,
                        "til_og_med" to periode.tilOgMed,
                        "fnr" to fnr.verdi,
                    ),
                ).map {
                    meldeperiodeOgGodkjentMeldekortFromRow(it)
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

    private fun meldeperiodeFromRow(row: Row, alias: String? = null): Meldeperiode {
        val col = prefixColumn(alias)
        return Meldeperiode(
            id = MeldeperiodeId.fromString(row.string(col("id"))),
            kjedeId = row.string(col("kjede_id")),
            fnr = Fnr.fromString(row.string(col("fnr"))),
            sakId = SakId.fromString(row.string(col("sak_id"))),
            saksnummer = row.string(col("saksnummer")),
            opprettet = row.localDateTime(col("opprettet")),
            fraOgMed = row.localDate(col("fra_og_med")),
            tilOgMed = row.localDate(col("til_og_med")),
            maksAntallDagerForPeriode = row.int(col("maks_antall_dager_for_periode")),
            girRett = objectMapper.readValue<Map<LocalDate, Boolean>>(row.string(col("gir_rett"))),
        )
    }

    private fun meldeperiodeOgGodkjentMeldekortFromRow(row: Row): MeldeperiodeOgGodkjentMeldekort {
        return MeldeperiodeOgGodkjentMeldekort(
            meldeperiode = meldeperiodeFromRow(row, alias = "m"),
            godkjentMeldekort = row.stringOrNull("gm.sak_id")?.let { GodkjentMeldekortRepo.godkjentMeldekortFromRow(row, alias = "gm") },
        )
    }
}

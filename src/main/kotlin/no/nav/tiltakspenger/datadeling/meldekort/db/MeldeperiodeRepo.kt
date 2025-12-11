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
            meldeperioder.filter { it.minstEnDagGirRettIPerioden }
                .forEach {
                    lagre(it, session)
                }

            meldeperioder.filterNot { it.minstEnDagGirRettIPerioden }
                .forEach {
                    if (finnesGodkjentMeldekortForMeldeperiode(it.sakId, it.kjedeId, session)) {
                        lagre(it, session)
                    } else {
                        log.info { "Sletter meldeperiode: sakId: ${it.sakId}, kjedeId: ${it.kjedeId}, id: ${it.id}" }
                        slett(it.sakId, it.kjedeId, session)
                    }
                }
        }
    }

    private fun finnesGodkjentMeldekortForMeldeperiode(
        sakId: SakId,
        kjedeId: String,
        session: Session,
    ): Boolean {
        return session.run(
            queryOf(
                """
                        select exists(select 1 from godkjent_meldekort where kjede_id = :kjede_id and sak_id = :sak_id)
                """.trimIndent(),
                mapOf(
                    "kjede_id" to kjedeId,
                    "sak_id" to sakId.toString(),
                ),
            ).map { row -> row.boolean("exists") }.asSingle,
        ) ?: throw RuntimeException("Kunne ikke avgjøre om godkjent meldekort finnes for sakId $sakId og kjedeId $kjedeId")
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
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        maks_antall_dager_for_periode,
                        gir_rett
                    ) values (
                        :id,
                        :kjede_id,
                        :sak_id,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        :maks_antall_dager_for_periode,
                        :gir_rett
                    ) on conflict (kjede_id, sak_id) do update set
                        id = :id,
                        sak_id = :sak_id,
                        opprettet = :opprettet,
                        fra_og_med = :fra_og_med,
                        til_og_med = :til_og_med,
                        maks_antall_dager_for_periode = :maks_antall_dager_for_periode,
                        gir_rett = :gir_rett
                """,
                "id" to meldeperiode.id.toString(),
                "kjede_id" to meldeperiode.kjedeId,
                "sak_id" to meldeperiode.sakId.toString(),
                "opprettet" to meldeperiode.opprettet,
                "fra_og_med" to meldeperiode.fraOgMed,
                "til_og_med" to meldeperiode.tilOgMed,
                "maks_antall_dager_for_periode" to meldeperiode.maksAntallDagerForPeriode,
                "gir_rett" to toPGObject(meldeperiode.girRett),
            ).asUpdate,
        )
        log.info { "Lagret meldeperiode med id ${meldeperiode.id}" }
    }

    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
    ): List<Meldeperiode> {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select m.*,
                      s.fnr as sak_fnr
                    from meldeperiode m join sak s on s.id = m.sak_id
                      where s.fnr = :fnr
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
                        s.fnr as "s.sak_fnr",
                        m.opprettet as "m.opprettet",
                        m.fra_og_med as "m.fra_og_med",
                        m.til_og_med as "m.til_og_med",
                        m.maks_antall_dager_for_periode as "m.maks_antall_dager_for_periode",
                        m.gir_rett as "m.gir_rett",
                        gm.meldekortbehandling_id as "gm.meldekortbehandling_id",
                        gm.kjede_id as "gm.kjede_id",
                        gm.sak_id as "gm.sak_id",
                        gm.meldeperiode_id as "gm.meldeperiode_id",
                        gm.mottatt_tidspunkt as "gm.mottatt_tidspunkt",
                        gm.vedtatt_tidspunkt as "gm.vedtatt_tidspunkt",
                        gm.behandlet_automatisk as "gm.behandlet_automatisk",
                        gm.korrigert as "gm.korrigert",
                        gm.fra_og_med as "gm.fra_og_med",
                        gm.til_og_med as "gm.til_og_med",
                        gm.meldekortdager as "gm.meldekortdager",
                        gm.journalpost_id as "gm.journalpost_id",
                        gm.totalt_belop as "gm.totalt_belop",
                        gm.total_differanse as "gm.total_differanse",
                        gm.barnetillegg as "gm.barnetillegg",
                        gm.opprettet as "gm.opprettet",
                        gm.sist_endret as "gm.sist_endret"
                    from meldeperiode m
                      join sak s on s.id = m.sak_id
                      left join godkjent_meldekort gm on m.sak_id = gm.sak_id and m.kjede_id = gm.kjede_id
                      where s.fnr = :fnr
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

    private fun meldeperiodeFromRow(row: Row, alias: String? = null): Meldeperiode {
        val col = prefixColumn(alias)
        return Meldeperiode(
            id = MeldeperiodeId.fromString(row.string(col("id"))),
            kjedeId = row.string(col("kjede_id")),
            sakId = SakId.fromString(row.string(col("sak_id"))),
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

package no.nav.tiltakspenger.datadeling.meldekort.infra

import arrow.core.toNonEmptyListOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import no.nav.tiltakspenger.datadeling.infra.db.prefixColumn
import no.nav.tiltakspenger.datadeling.infra.db.toPGObject
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandling
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandlingRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime

class GodkjentMeldekortbehandlingPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : GodkjentMeldekortbehandlingRepo {
    val log = KotlinLogging.logger { }

    companion object {
        fun godkjentMeldekortbehandlingFromRow(row: Row, alias: String? = null): GodkjentMeldekortbehandling {
            val col = prefixColumn(alias)
            return GodkjentMeldekortbehandling(
                meldekortbehandlingId = MeldekortId.fromString(row.string(col("meldekortbehandling_id"))),
                sakId = SakId.fromString(row.string(col("sak_id"))),
                meldeperioder = objectMapper.readValue<List<MeldeperiodeDb>>(row.string(col("meldeperioder")))
                    .map { it.toDomain() }
                    .toNonEmptyListOrThrow(),
                vedtattTidspunkt = row.localDateTime(col("vedtatt_tidspunkt")),
                behandletAutomatisk = row.boolean(col("behandlet_automatisk")),
                fraOgMed = row.localDate(col("fra_og_med")),
                tilOgMed = row.localDate(col("til_og_med")),
                journalpostId = row.string(col("journalpost_id")),
                totaltBelop = row.int(col("totalt_belop")),
                totalDifferanse = row.intOrNull(col("total_differanse")),
                barnetillegg = row.boolean(col("barnetillegg")),
                opprettet = row.localDateTime(col("opprettet")),
                sistEndret = row.localDateTime(col("sist_endret")),
            )
        }
    }

    override fun lagre(meldekort: GodkjentMeldekortbehandling) {
        sessionFactory.withTransaction { session ->
            session.run(
                sqlQuery(
                    """
                    insert into godkjent_meldekort (
                        meldekortbehandling_id,
                        sak_id,
                        meldeperioder,
                        vedtatt_tidspunkt,
                        behandlet_automatisk,
                        fra_og_med,
                        til_og_med,
                        journalpost_id,
                        totalt_belop,
                        total_differanse,
                        barnetillegg,
                        opprettet,
                        sist_endret
                    ) values (
                        :meldekortbehandling_id,
                        :sak_id,
                        :meldeperioder,
                        :vedtatt_tidspunkt,
                        :behandlet_automatisk,
                        :fra_og_med,
                        :til_og_med,
                        :journalpost_id,
                        :totalt_belop,
                        :total_differanse,
                        :barnetillegg,
                        :opprettet,
                        :sist_endret
                    )
                    on conflict (meldekortbehandling_id) do update set
                        sak_id = :sak_id,
                        meldeperioder = :meldeperioder,
                        vedtatt_tidspunkt = :vedtatt_tidspunkt,
                        behandlet_automatisk = :behandlet_automatisk,
                        fra_og_med = :fra_og_med,
                        til_og_med = :til_og_med,
                        journalpost_id = :journalpost_id,
                        totalt_belop = :totalt_belop,
                        total_differanse = :total_differanse,
                        barnetillegg = :barnetillegg,
                        opprettet = :opprettet,
                        sist_endret = :sist_endret
                """,
                    "meldekortbehandling_id" to meldekort.meldekortbehandlingId.toString(),
                    "sak_id" to meldekort.sakId.toString(),
                    "meldeperioder" to toPGObject(meldekort.meldeperioder.map { MeldeperiodeDb.fromDomain(it) }),
                    "vedtatt_tidspunkt" to meldekort.vedtattTidspunkt,
                    "behandlet_automatisk" to meldekort.behandletAutomatisk,
                    "fra_og_med" to meldekort.fraOgMed,
                    "til_og_med" to meldekort.tilOgMed,
                    "journalpost_id" to meldekort.journalpostId,
                    "totalt_belop" to meldekort.totaltBelop,
                    "total_differanse" to meldekort.totalDifferanse,
                    "barnetillegg" to meldekort.barnetillegg,
                    "opprettet" to meldekort.opprettet,
                    "sist_endret" to meldekort.sistEndret,
                ).asUpdate,
            )
        }
        log.info { "Lagret godkjent meldekort for meldekortbehandlingId ${meldekort.meldekortbehandlingId}, kjedeIder ${meldekort.meldeperioder.map { it.kjedeId }} for sakId ${meldekort.sakId}" }
    }

    override fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
    ): List<GodkjentMeldekortbehandling> {
        return sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    select gm.*,
                      s.fnr as sak_fnr
                    from godkjent_meldekort gm join sak s on s.id = gm.sak_id
                    where s.fnr = :fnr
                      and fra_og_med <= :til_og_med
                      and til_og_med >= :fra_og_med
                    """.trimIndent(),
                    "fra_og_med" to periode.fraOgMed,
                    "til_og_med" to periode.tilOgMed,
                    "fnr" to fnr.verdi,
                ).map {
                    godkjentMeldekortbehandlingFromRow(it)
                }.asList,
            )
        }
    }
}

private data class MeldeperiodeDb(
    val kjedeId: String,
    val meldeperiodeId: String,
    val korrigert: Boolean,
    val meldekortdager: List<MeldekortDagDb>,
    val totaltBelop: Int,
    val totalDifferanse: Int?,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val mottattTidspunkt: LocalDateTime?,
) {
    fun toDomain() = GodkjentMeldekortbehandling.Meldeperiode(
        kjedeId = kjedeId,
        meldeperiodeId = meldeperiodeId,
        korrigert = korrigert,
        meldekortdager = meldekortdager.map { it.toDomain() }.toNonEmptyListOrThrow(),
        totaltBelop = totaltBelop,
        totalDifferanse = totalDifferanse,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        mottattTidspunkt = mottattTidspunkt,
    )

    companion object {
        fun fromDomain(meldeperiode: GodkjentMeldekortbehandling.Meldeperiode) = MeldeperiodeDb(
            kjedeId = meldeperiode.kjedeId,
            meldeperiodeId = meldeperiode.meldeperiodeId,
            korrigert = meldeperiode.korrigert,
            meldekortdager = meldeperiode.meldekortdager.map { MeldekortDagDb.fromDomain(it) },
            totaltBelop = meldeperiode.totaltBelop,
            totalDifferanse = meldeperiode.totalDifferanse,
            fraOgMed = meldeperiode.fraOgMed,
            tilOgMed = meldeperiode.tilOgMed,
            mottattTidspunkt = meldeperiode.mottattTidspunkt,
        )
    }
}

private data class MeldekortDagDb(
    val dato: LocalDate,
    val status: GodkjentMeldekortbehandling.MeldekortDag.MeldekortDagStatus,
    val reduksjon: GodkjentMeldekortbehandling.MeldekortDag.Reduksjon,
) {
    fun toDomain() = GodkjentMeldekortbehandling.MeldekortDag(
        dato = dato,
        status = status,
        reduksjon = reduksjon,
    )

    companion object {
        fun fromDomain(meldekortDag: GodkjentMeldekortbehandling.MeldekortDag) = MeldekortDagDb(
            dato = meldekortDag.dato,
            status = meldekortDag.status,
            reduksjon = meldekortDag.reduksjon,
        )
    }
}

-- 1. Legg til ny meldeperioder-kolonne.
ALTER TABLE godkjent_meldekort
    ADD COLUMN meldeperioder jsonb NOT NULL DEFAULT '[]'::jsonb;

-- 2. Backfyll meldeperioder-lista fra de gamle kolonnene.
UPDATE godkjent_meldekort
SET meldeperioder = jsonb_build_array(
    jsonb_build_object(
        'kjedeId', kjede_id,
        'meldeperiodeId', meldeperiode_id,
        'korrigert', korrigert,
        'meldekortdager', meldekortdager,
        'totaltBelop', totalt_belop,
        'totalDifferanse', total_differanse,
        'fraOgMed', to_char(fra_og_med, 'YYYY-MM-DD'),
        'tilOgMed', to_char(til_og_med, 'YYYY-MM-DD')
    )
)
WHERE meldeperioder = '[]'::jsonb;

-- Kolonnen skal alltid settes eksplisitt av applikasjonen etter backfyll.
ALTER TABLE godkjent_meldekort
    ALTER COLUMN meldeperioder DROP DEFAULT;

-- 3. Fjern de gamle kolonnene. Data ligger nå i meldeperioder-lista.
-- Dropping av kjede_id fjerner samtidig foreign key mot meldeperiode og
-- den sammensatte indeksen (kjede_id, sak_id) automatisk.
ALTER TABLE godkjent_meldekort
    DROP COLUMN kjede_id,
    DROP COLUMN meldeperiode_id,
    DROP COLUMN meldekortdager,
    DROP COLUMN korrigert;

-- 4. Nye indekser for oppslag mot meldeperioder-lista.
CREATE INDEX idx_godkjent_meldekort_sak_id ON godkjent_meldekort (sak_id);
CREATE INDEX idx_godkjent_meldekort_meldeperioder ON godkjent_meldekort USING gin (meldeperioder);

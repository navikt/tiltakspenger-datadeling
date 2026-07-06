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

-- 3. Behold de gamle kolonnene inntil videre (til konsumenter/rollback er avklart),
-- men fjern NOT NULL-constraintene siden applikasjonen ikke lenger skriver dem.
-- Uten dette ville hver insert feile med NOT NULL-brudd (23502).
-- Den sammensatte foreign keyen (kjede_id, sak_id) mot meldeperiode og indeksen
-- (kjede_id, sak_id) beholdes; de er harmløse (FK håndheves ikke når kjede_id er NULL).
ALTER TABLE godkjent_meldekort
    ALTER COLUMN kjede_id DROP NOT NULL,
    ALTER COLUMN meldeperiode_id DROP NOT NULL,
    ALTER COLUMN meldekortdager DROP NOT NULL,
    ALTER COLUMN korrigert DROP NOT NULL;

-- 4. Nye indekser for oppslag mot meldeperioder-lista.
CREATE INDEX idx_godkjent_meldekort_sak_id ON godkjent_meldekort (sak_id);
CREATE INDEX idx_godkjent_meldekort_meldeperioder ON godkjent_meldekort USING gin (meldeperioder);

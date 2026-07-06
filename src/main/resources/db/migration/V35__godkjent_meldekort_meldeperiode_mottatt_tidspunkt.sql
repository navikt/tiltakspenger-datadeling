-- Flytt mottattTidspunkt inn i hvert element i meldeperioder-lista.
-- V34 backfylte meldeperioder uten mottattTidspunkt, og mottatt_tidspunkt lå på
-- behandlingsnivå. Legg verdien fra kolonnen inn i hvert meldeperiode-element for
-- eksisterende rader. Nye rader skrives av applikasjonen med feltet allerede satt.
UPDATE godkjent_meldekort gm
SET meldeperioder = (
    SELECT jsonb_agg(
        elem || jsonb_build_object(
            'mottattTidspunkt',
            to_char(gm.mottatt_tidspunkt, 'YYYY-MM-DD"T"HH24:MI:SS.US')
        )
    )
    FROM jsonb_array_elements(gm.meldeperioder) AS elem
)
WHERE jsonb_array_length(gm.meldeperioder) > 0
  AND NOT ((gm.meldeperioder -> 0) ? 'mottattTidspunkt');

CREATE INDEX idx_meldeperiode_sak_id ON meldeperiode (sak_id);
CREATE INDEX idx_godkjent_meldekort_kjede_id_sak_id ON godkjent_meldekort (kjede_id, sak_id);


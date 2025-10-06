CREATE TABLE meldeperiode
(
    id                            varchar PRIMARY KEY,
    kjede_id                      varchar                  NOT NULL,
    sak_id                        varchar                  NOT NULL,
    saksnummer                    varchar                  NOT NULL,
    fnr                           varchar                  NOT NULL,
    opprettet                     timestamp with time zone NOT NULL,
    fra_og_med                    DATE                     NOT NULL,
    til_og_med                    DATE                     NOT NULL,
    maks_antall_dager_for_periode INTEGER                  NOT NULL,
    gir_rett                      JSONB                    NOT NULL,

    CONSTRAINT unique_kjede_id_opprettet UNIQUE (sak_id, kjede_id)
);

CREATE INDEX idx_meldeperiode_fnr_fra_og_med_til_og_med ON meldeperiode (fnr, fra_og_med, til_og_med);
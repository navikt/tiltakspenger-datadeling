CREATE TABLE godkjent_meldekort
(
    kjede_id             varchar                  NOT NULL,
    sak_id               varchar                  NOT NULL,
    meldeperiode_id      varchar                  NOT NULL,
    fnr                  varchar                  NOT NULL,
    saksnummer           varchar                  NOT NULL,
    mottatt_tidspunkt    timestamp with time zone,
    vedtatt_tidspunkt    timestamp with time zone NOT NULL,
    behandlet_automatisk boolean                  not null,
    korrigert            boolean                  not null,
    fra_og_med           date                     not null,
    til_og_med           date                     not null,
    meldekortdager       jsonb                    not null,
    opprettet            timestamp with time zone not null,
    sist_endret          timestamp with time zone not null,
    primary key (kjede_id, sak_id),
    FOREIGN KEY (kjede_id, sak_id) REFERENCES meldeperiode (kjede_id, sak_id) on delete cascade
);

CREATE INDEX idx_godkjent_meldekort_fnr_fra_og_med_til_og_med ON godkjent_meldekort (fnr, fra_og_med, til_og_med);
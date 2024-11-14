CREATE TABLE behandling
(
    behandling_id                          VARCHAR     NOT NULL,
    fnr                                    VARCHAR     NOT NULL,
    sak_id                                 VARCHAR     NOT NULL,
    saksnummer                             VARCHAR     NOT NULL,
    søknad_journalpost_id                  VARCHAR     NOT NULL,
    fra_og_med                             DATE        NOT NULL,
    til_og_med                             DATE        NOT NULL,
    behandling_status                      VARCHAR     NOT NULL,
    saksbehandler                          VARCHAR     NULL,
    beslutter                              VARCHAR     NULL,
    tiltaksdeltagelse                      JSONB       NOT NULL,
    iverksatt_tidspunkt                    TIMESTAMPTZ NULL,
    opprettet_tidspunkt_saksbehandling_api TIMESTAMPTZ NOT NULL,
    mottatt_tidspunkt_datadeling           TIMESTAMPTZ NOT NULL,
    kilde                                  VARCHAR     NOT NULL,
    PRIMARY KEY (behandling_id, kilde)
);

CREATE INDEX idx_behandling_fnr_fra_og_med_til_og_med ON behandling (fnr, fra_og_med, til_og_med);
CREATE INDEX idx_behandling_fnr ON behandling (fnr);

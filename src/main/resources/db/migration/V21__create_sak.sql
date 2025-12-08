create table sak
(
    id          VARCHAR PRIMARY KEY,
    fnr         VARCHAR     NOT NULL,
    saksnummer  VARCHAR     NOT NULL UNIQUE,
    opprettet   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sak_fnr ON sak (fnr);
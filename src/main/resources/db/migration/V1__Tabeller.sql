DO
$$
    BEGIN
        IF
            EXISTS
                (SELECT 1 from pg_roles where rolname = 'cloudsqliamuser')
        THEN
            GRANT USAGE ON SCHEMA public TO cloudsqliamuser;
            GRANT
                SELECT
                ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
            ALTER
                DEFAULT PRIVILEGES IN SCHEMA public GRANT
                SELECT
                ON TABLES TO cloudsqliamuser;
        END IF;
    END
$$;

CREATE TABLE rammevedtak
(
    vedtak_id                     VARCHAR NOT NULL,
    sak_id                        VARCHAR NOT NULL,
    saksnummer                    VARCHAR NOT NULL,
    fnr                           VARCHAR NOT NULL,
    fra_og_med                    VARCHAR NOT NULL,
    til_og_med                    VARCHAR NOT NULL,
    antall_dager_per_meldeperiode INTEGER NOT NULL,
    meldeperiodens_lengde         INTEGER NOT NULL,
    dagsats_tiltakspenger         INTEGER NOT NULL,
    dagsats_barnetillegg          INTEGER NOT NULL,
    antall_barn                   INTEGER NOT NULL,
    tiltaksgjennomføring_id       VARCHAR NOT NULL,
    rettighet                     VARCHAR NOT NULL,
    kilde                         VARCHAR NOT NULL,
    opprettet_tidspunkt           TIMESTAMPTZ NOT NULL,
    mottatt_tidspunkt             TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (vedtak_id, kilde)
);

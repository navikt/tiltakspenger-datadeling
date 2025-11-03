alter table behandling
    add column if not exists behandlingstype varchar not null default 'SOKNADSBEHANDLING';

alter table behandling
    add column if not exists sist_endret timestamptz;

update behandling
set sist_endret = mottatt_tidspunkt_datadeling;

alter table behandling
    alter column sist_endret set not null;
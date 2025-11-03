alter table behandling
    alter column fra_og_med drop not null;

alter table behandling
    alter column til_og_med drop not null;

alter table behandling
    drop column søknad_journalpost_id;
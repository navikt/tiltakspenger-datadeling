alter table behandling
    add constraint behandling_sak_id_fkey foreign key (sak_id) references sak (id)
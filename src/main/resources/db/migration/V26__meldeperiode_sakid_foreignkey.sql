alter table meldeperiode
    add constraint meldeperiode_sak_id_fkey foreign key (sak_id) references sak (id)
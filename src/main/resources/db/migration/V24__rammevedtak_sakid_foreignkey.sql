alter table rammevedtak
    add constraint rammevedtak_sak_id_fkey foreign key (sak_id) references sak (id)
alter table rammevedtak
    drop constraint rammevedtak_pkey,
    add constraint rammevedtak_pkey primary key (vedtak_id)
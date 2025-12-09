alter table godkjent_meldekort
    add constraint godkjent_meldekort_sak_id_fkey foreign key (sak_id) references sak (id)
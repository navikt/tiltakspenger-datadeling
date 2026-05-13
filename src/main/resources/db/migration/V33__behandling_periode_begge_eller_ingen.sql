alter table behandling
    add constraint behandling_fra_og_med_til_og_med_begge_eller_ingen
        check (
            (fra_og_med is null and til_og_med is null)
            or (fra_og_med is not null and til_og_med is not null)
        );


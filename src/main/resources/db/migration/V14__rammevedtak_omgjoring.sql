ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS virkningsperiode_fra_og_med date DEFAULT null;
ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS virkningsperiode_til_og_med date DEFAULT null;
ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS innvilgelsesperiode jsonb DEFAULT null;
ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS omgjør_rammevedtak_id varchar DEFAULT null;
ALTER TABLE rammevedtak ADD COLUMN IF NOT EXISTS omgjort_av_rammevedtak_id varchar DEFAULT null;

CREATE INDEX idx_vedtak_fnr_fra_og_med_til_og_med ON rammevedtak (fnr, fra_og_med, til_og_med);
CREATE INDEX idx_vedtak_fnr ON rammevedtak (fnr);
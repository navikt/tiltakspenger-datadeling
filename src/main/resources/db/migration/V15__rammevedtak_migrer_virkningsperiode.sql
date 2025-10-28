UPDATE rammevedtak
SET virkningsperiode_fra_og_med = CAST(SPLIT_PART(fra_og_med, ' ', 1) AS DATE)
WHERE virkningsperiode_fra_og_med IS NULL
  AND fra_og_med IS NOT NULL;

UPDATE rammevedtak
SET virkningsperiode_til_og_med = CAST(SPLIT_PART(til_og_med, ' ', 1) AS DATE)
WHERE virkningsperiode_til_og_med IS NULL
  AND til_og_med IS NOT NULL;
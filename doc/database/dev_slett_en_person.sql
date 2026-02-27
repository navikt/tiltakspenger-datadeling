WITH sak_to_delete AS (SELECT id FROM sak WHERE id = 'lim-inn-sak-id-her'),
     del_godkjent_meldekort AS (DELETE FROM godkjent_meldekort WHERE sak_id IN (SELECT id FROM sak_to_delete)),
    del_rammevedtak        AS (DELETE FROM rammevedtak        WHERE sak_id IN (SELECT id FROM sak_to_delete)),
    del_meldeperiode       AS (DELETE FROM meldeperiode       WHERE sak_id IN (SELECT id FROM sak_to_delete)),
    del_behandling         AS (DELETE FROM behandling         WHERE sak_id IN (SELECT id FROM sak_to_delete))
DELETE FROM sak WHERE id IN (SELECT id FROM sak_to_delete);
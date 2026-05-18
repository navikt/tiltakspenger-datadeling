package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr

interface HentSakRepo {
    fun hentSakForVedtakSak(fnr: Fnr): SakForVedtakSak?
}

package no.nav.tiltakspenger.datadeling.sak

import arrow.core.Either

class MottaSakService(
    private val sakRepo: SakRepo,
) {
    fun motta(kommando: MottaSakKommando): Either<KanIkkeMottaSak, Unit> {
        return Either.catch { sakRepo.lagre(kommando.nySak.tilSak()) }
            .mapLeft { KanIkkeMottaSak.UkjentFeil }
    }
}

sealed interface KanIkkeMottaSak {
    data object UkjentFeil : KanIkkeMottaSak
}

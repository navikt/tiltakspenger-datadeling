package no.nav.tiltakspenger.datadeling.domene

import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller

/**
 * @param brukernavn Brukernavn til systembruker (azp_name for Entra ID). Kan ikke brukes til autentisering.
 */
data class Systembruker(
    override val brukernavn: String,
    override val roller: Systembrukerroller,
) : GenerellSystembruker<Systembrukerrolle, Systembrukerroller>

enum class Systembrukerrolle : GenerellSystembrukerrolle {
    LAGRE_TILTAKSPENGER_HENDELSER,
    LES_VEDTAK,
    LES_BEHANDLING,
}

data class Systembrukerroller(
    override val value: Set<Systembrukerrolle>,
) : GenerellSystembrukerroller<Systembrukerrolle>, Set<Systembrukerrolle> by value {
    override fun harRolle(rolle: Systembrukerrolle): Boolean = contains(rolle)
    constructor(vararg roller: Systembrukerrolle) : this(roller.toSet())
    constructor(roller: Collection<Systembrukerrolle>) : this(roller.toSet())

    fun kanLagreTiltakspengerHendelser(): Boolean = value.contains(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)
    fun kanLeseVedtak(): Boolean = value.contains(Systembrukerrolle.LES_VEDTAK)
    fun kanLeseBehandlinger(): Boolean = value.contains(Systembrukerrolle.LES_BEHANDLING)
}

package no.nav.tiltakspenger.datadeling.testutils

import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import java.time.Instant
import kotlin.collections.map
import kotlin.collections.set
import kotlin.to

class TexasClientFake : TexasClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, Systembruker>())

    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TexasIntrospectionResponse {
        return godkjentResponse(token)
    }

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
        rewriteAudienceTarget: Boolean,
    ): AccessToken {
        return accessToken()
    }

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: IdentityProvider,
    ): AccessToken {
        return accessToken()
    }

    fun leggTilSystembruker(token: String, systembruker: Systembruker) {
        data.get()[token] = systembruker
    }

    private fun accessToken(): AccessToken = AccessToken(
        token = "asdf",
        expiresAt = Instant.now().plusSeconds(3600),
        invaliderCache = { },
    )

    private fun godkjentResponse(token: String): TexasIntrospectionResponse {
        val systembruker = data.get()[token] ?: return TexasIntrospectionResponse(
            active = false,
            error = "Ingen gyldig token",
            groups = null,
            roles = null,
            other = emptyMap(),
        )
        return TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = null,
            roles = getRoles(systembruker),
            other = mutableMapOf(
                "azp_name" to systembruker.klientnavn,
                "azp" to systembruker.klientId,
                "idtyp" to "app",
            ),
        )
    }

    private fun getRoles(systembruker: Systembruker): List<String> {
        return systembruker.roller.value.map { rolle ->
            when (rolle) {
                Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER -> "lagre-tiltakspenger-hendelser"
                Systembrukerrolle.LES_BEHANDLING -> "les-behandling"
                Systembrukerrolle.LES_VEDTAK -> "les-vedtak"
                Systembrukerrolle.LES_MELDEKORT -> "les-meldekort"
            }
        }
    }
}

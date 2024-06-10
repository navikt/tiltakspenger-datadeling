package no.nav.tiltakspenger.datadeling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.tiltakspenger.datadeling.auth.AzureTokenProvider

enum class Profile {
    LOCAL, DEV, PROD
}

object Configuration {
    fun applicationProfile() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" -> Profile.DEV
            "prod-gcp" -> Profile.PROD
            else -> Profile.LOCAL
        }

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8080.toString(),
            "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
            "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
            "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
            "AZURE_OPENID_CONFIG_ISSUER" to System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            "AZURE_OPENID_CONFIG_JWKS_URI" to System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            "logback.configurationFile" to "logback.xml",
        ),
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8099.toString(),
            "application.profile" to Profile.LOCAL.toString(),
            "logback.configurationFile" to "logback.local.xml",
            "AZURE_APP_CLIENT_ID" to "tiltakspenger-datadeling",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_APP_WELL_KNOWN_URL" to "http://host.docker.internal:6969/azure/.well-known/openid-configuration",
            "AZURE_OPENID_CONFIG_ISSUER" to "http://host.docker.internal:6969/azure",
            "AZURE_OPENID_CONFIG_JWKS_URI" to "http://host.docker.internal:6969/azure/jwks",
            "VEDTAK_SCOPE" to "localhost",
            "VEDTAK_URL" to "http://localhost:8080",
            "ARENA_SCOPE" to "arena",
            "ARENA_URL" to "http://localhost:8097",
        ),
    )

    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "VEDTAK_SCOPE" to "api://dev-gcp.tpts.tiltakspenger-vedtak/.default",
            "VEDTAK_URL" to "http://tiltakspenger-vedtak.tpts",
            "ARENA_SCOPE" to "api://dev-gcp.tpts.tiltakspenger-arena/.default",
            "ARENA_URL" to "http://tiltakspenger-arena.tpts",
        ),
    )

    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
            "VEDTAK_SCOPE" to "api://prod-gcp.tpts.tiltakspenger-vedtak/.default",
            "VEDTAK_URL" to "https://tiltakspenger-vedtak.tpts",
            "ARENA_SCOPE" to "api://prod-gcp.tpts.tiltakspenger-arena/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.tpts",
        ),
    )

    private val composeProperties = ConfigurationMap(
        mapOf(
            "logback.configurationFile" to "logback.local.xml",
            "VEDTAK_SCOPE" to System.getenv("VEDTAK_SCOPE"),
            "VEDTAK_URL" to System.getenv("VEDTAK_URL"),
            "ARENA_SCOPE" to System.getenv("ARENA_SCOPE"),
            "ARENA_URL" to System.getenv("ARENA_URL"),
        ),
    )

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

        "prod-gcp" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

        "compose" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding composeProperties overriding defaultProperties

        else -> {
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
        }
    }

    data class ClientConfig(
        val baseUrl: String,
    )

    fun vedtakClientConfig(baseUrl: String = config()[Key("VEDTAK_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun arenaClientConfig(baseUrl: String = config()[Key("ARENA_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun oauthConfigVedtak(
        scope: String = config()[Key("VEDTAK_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun oauthConfigArena(
        scope: String = config()[Key("ARENA_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]
}

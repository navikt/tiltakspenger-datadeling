package no.nav.tiltakspenger.datadeling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

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
            "AZURE_OPENID_CONFIG_ISSUER" to System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            "AZURE_OPENID_CONFIG_JWKS_URI" to System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
            "logback.configurationFile" to "logback.xml",
        ),
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8082.toString(),
            "application.profile" to Profile.LOCAL.toString(),
            "logback.configurationFile" to "logback.local.xml",
            "AZURE_APP_CLIENT_ID" to "tiltakspenger-datadeling",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_OPENID_CONFIG_ISSUER" to "http://host.docker.internal:6969/azure",
            "AZURE_OPENID_CONFIG_JWKS_URI" to "http://host.docker.internal:6969/azure/jwks",
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://host.docker.internal:6969/default/token",
            "VEDTAK_SCOPE" to "localhost",
            "VEDTAK_URL" to "http://localhost:8080",
            "ARENA_SCOPE" to "arena",
            "ARENA_URL" to "http://localhost:8097",
            "DB_JDBC_URL" to "jdbc:postgresql://localhost:5434/datadeling?user=postgres&password=test",
        ),
    )

    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "VEDTAK_SCOPE" to "api://dev-gcp.tpts.tiltakspenger-saksbehandling-api/.default",
            "VEDTAK_URL" to "http://tiltakspenger-saksbehandling-api.tpts",
            "ARENA_SCOPE" to "api://dev-fss.tpts.tiltakspenger-arena/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.dev-fss-pub.nais.io",
        ),
    )

    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
            "VEDTAK_SCOPE" to "api://prod-gcp.tpts.tiltakspenger-saksbehandling-api/.default",
            "VEDTAK_URL" to "https://tiltakspenger-saksbehandling-api.tpts",
            "ARENA_SCOPE" to "api://prod-fss.tpts.tiltakspenger-arena/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.prod-fss-pub.nais.io",
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

    val jdbcUrl: String by lazy { config()[Key("DB_JDBC_URL", stringType)] }

    data class ClientConfig(
        val baseUrl: String,
    )

    fun vedtakClientConfig(baseUrl: String = config()[Key("VEDTAK_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun arenaClientConfig(baseUrl: String = config()[Key("ARENA_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    val azureAppClientId: String by lazy { config()[Key("AZURE_APP_CLIENT_ID", stringType)] }
    val azureAppClientSecret: String by lazy { config()[Key("AZURE_APP_CLIENT_SECRET", stringType)] }

    val arenaScope: String by lazy { config()[Key("ARENA_SCOPE", stringType)] }
    val vedtakScope: String by lazy { config()[Key("VEDTAK_SCOPE", stringType)] }

    /** Samme som hvis man gjør en get til AZURE_APP_WELL_KNOWN_URL og plukker ut 'token_endpoint' */
    val azureOpenidConfigTokenEndpoint: String by lazy { config()[Key("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", stringType)] }
    val azureOpenidConfigJwksUri: String by lazy { config()[Key("AZURE_OPENID_CONFIG_JWKS_URI", stringType)] }
    val azureOpenidConfigIssuer: String by lazy { config()[Key("AZURE_OPENID_CONFIG_ISSUER", stringType)] }

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]
}

package no.nav.tiltakspenger.datadeling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private const val APPLICATION_NAME = "tiltakspenger-datadeling"
const val KAFKA_CONSUMER_GROUP_ID = "$APPLICATION_NAME-consumer"

enum class Profile {
    LOCAL,
    DEV,
    PROD,
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
            "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
            "logback.configurationFile" to "logback.xml",
            "IDENTHENDELSE_TOPIC" to "tpts.identhendelse-v1",
            "ARENA_SCOPE" to System.getenv("ARENA_SCOPE"),
            "ARENA_URL" to System.getenv("ARENA_URL"),
            "NAIS_TOKEN_ENDPOINT" to System.getenv("NAIS_TOKEN_ENDPOINT"),
            "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
            "NAIS_TOKEN_EXCHANGE_ENDPOINT" to System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
        ),
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8082.toString(),
            "application.profile" to Profile.LOCAL.toString(),
            "logback.configurationFile" to "logback.local.xml",
            "ARENA_SCOPE" to "arena",
            "ARENA_URL" to "http://localhost:8097",
            "DB_JDBC_URL" to "jdbc:postgresql://localhost:5434/datadeling?user=postgres&password=test",
            "NAIS_TOKEN_ENDPOINT" to "http://localhost:7165/api/v1/token",
            "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to "http://localhost:7165/api/v1/introspect",
            "NAIS_TOKEN_EXCHANGE_ENDPOINT" to "http://localhost:7165/api/v1/token/exchange",
        ),
    )

    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
        ),
    )

    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
        ),
    )

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

        "prod-gcp" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

        else -> {
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
        }
    }

    val jdbcUrl: String by lazy { config()[Key("DB_JDBC_URL", stringType)] }

    val naisTokenIntrospectionEndpoint: String by lazy { config()[Key("NAIS_TOKEN_INTROSPECTION_ENDPOINT", stringType)] }
    val naisTokenEndpoint: String by lazy { config()[Key("NAIS_TOKEN_ENDPOINT", stringType)] }
    val tokenExchangeEndpoint: String by lazy { config()[Key("NAIS_TOKEN_EXCHANGE_ENDPOINT", stringType)] }

    val arenaUrl: String by lazy { config()[Key("ARENA_URL", stringType)] }
    val arenaScope: String by lazy { config()[Key("ARENA_SCOPE", stringType)] }

    val identhendelseTopic: String by lazy { config()[Key("IDENTHENDELSE_TOPIC", stringType)] }

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun isNais() = applicationProfile() != Profile.LOCAL
}

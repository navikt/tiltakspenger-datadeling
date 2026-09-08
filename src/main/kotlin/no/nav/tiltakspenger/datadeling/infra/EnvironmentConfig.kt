package no.nav.tiltakspenger.datadeling.infra

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

sealed interface EnvironmentConfig {
    val profile: Profile
    val httpPort: Int
    val logbackConfigurationFile: String

    val electorPath: String
    val dbJdbcUrl: String

    /** Til sikkerlogg-henvisningen; satt av nais i podene, null lokalt (da blir henvisningen ren tekst uten lenke). */
    val naisAppName: String?
    val gcpTeamProjectId: String?

    val tokenEndpoint: String
    val tokenIntrospectionEndpoint: String
    val tokenExchangeEndpoint: String

    val arenaUrl: String
    val arenaScope: String

    val identhendelseTopic: String
    val oboYtelserTopic: String
}

data object LocalConfig : EnvironmentConfig {
    override val profile = Profile.LOCAL
    override val httpPort = 8082
    override val logbackConfigurationFile = "logback.local.xml"

    // Brukes ikke lokalt
    override val electorPath = ""
    override val dbJdbcUrl = "jdbc:postgresql://localhost:5434/datadeling?user=postgres&password=test"

    override val naisAppName: String? = null
    override val gcpTeamProjectId: String? = null

    override val tokenEndpoint = "http://localhost:7165/api/v1/token"
    override val tokenIntrospectionEndpoint = "http://localhost:7165/api/v1/introspect"
    override val tokenExchangeEndpoint = "http://localhost:7165/api/v1/token/exchange"

    override val arenaUrl = "http://localhost:8097"
    override val arenaScope = "arena"

    override val identhendelseTopic = "tpts.identhendelse-v1"
    override val oboYtelserTopic = "obo.ytelser-v1"
}

data object DevConfig : EnvironmentConfig {
    override val profile = Profile.DEV
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.xml"

    override val electorPath: String = System.getenv("ELECTOR_PATH")
    override val dbJdbcUrl: String = System.getenv("DB_JDBC_URL")

    override val naisAppName: String? = System.getenv("NAIS_APP_NAME")
    override val gcpTeamProjectId: String? = System.getenv("GCP_TEAM_PROJECT_ID")

    override val tokenEndpoint: String = System.getenv("NAIS_TOKEN_ENDPOINT")
    override val tokenIntrospectionEndpoint: String = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT")
    override val tokenExchangeEndpoint: String = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT")

    override val arenaUrl = "https://tiltakspenger-arena.dev-fss-pub.nais.io"
    override val arenaScope = "api://dev-fss.tpts.tiltakspenger-arena/.default"

    override val identhendelseTopic = "tpts.identhendelse-v1"
    override val oboYtelserTopic = "obo.ytelser-v1"
}

data object ProdConfig : EnvironmentConfig {
    override val profile = Profile.PROD
    override val httpPort = 8080
    override val logbackConfigurationFile = "logback.xml"

    override val electorPath: String = System.getenv("ELECTOR_PATH")
    override val dbJdbcUrl: String = System.getenv("DB_JDBC_URL")

    override val naisAppName: String? = System.getenv("NAIS_APP_NAME")
    override val gcpTeamProjectId: String? = System.getenv("GCP_TEAM_PROJECT_ID")

    override val tokenEndpoint: String = System.getenv("NAIS_TOKEN_ENDPOINT")
    override val tokenIntrospectionEndpoint: String = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT")
    override val tokenExchangeEndpoint: String = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT")

    override val arenaUrl = "https://tiltakspenger-arena.prod-fss-pub.nais.io"
    override val arenaScope = "api://prod-fss.tpts.tiltakspenger-arena/.default"

    override val identhendelseTopic = "tpts.identhendelse-v1"
    override val oboYtelserTopic = "obo.ytelser-v1"
}

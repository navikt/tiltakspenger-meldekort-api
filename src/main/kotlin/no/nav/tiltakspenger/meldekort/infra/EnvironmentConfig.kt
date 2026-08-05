package no.nav.tiltakspenger.meldekort.infra

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

    val saksbehandlingApiAudience: String
    val saksbehandlingApiUrl: String

    val dokarkivScope: String
    val dokarkivUrl: String

    val pdfgenrsUrl: String

    val arenaMeldekortServiceAudience: String
    val arenaMeldekortServiceUrl: String

    val meldekortFrontendUrl: String

    val varselHendelseTopic: String
    val microfrontendTopic: String
    val identhendelseTopic: String
}

data object LocalConfig : EnvironmentConfig {
    /**
     * Benyttes kun dersom appen kjører lokalt med "prod-main" i Application.kt.
     * Ved kjøring via LokalMain.kt vil normalt fake-klienter benyttes istedenfor å kalle disse url'ene.
     *
     * Wiremock kan kjøres opp via docker-compose i meta-repoet.
     */
    private const val WIREMOCK_URL = "http://host.docker.internal:8091"

    override val profile = Profile.LOCAL
    override val httpPort = 8083
    override val logbackConfigurationFile = "logback.local.xml"

    // Brukes ikke lokalt
    override val electorPath = ""

    /**
     * LokalMain setter DB_JDBC_URL som system property når postgres startes i testcontainers-modus (tilfeldig port), derfor må denne leses på nytt hver gang.
     */
    override val dbJdbcUrl: String
        get() = System.getProperty("DB_JDBC_URL")
            ?: "jdbc:postgresql://localhost:5435/meldekort?user=postgres&password=test"

    override val naisAppName: String? = null
    override val gcpTeamProjectId: String? = null

    override val tokenEndpoint = "http://localhost:7164/api/v1/token"
    override val tokenIntrospectionEndpoint = "http://localhost:7164/api/v1/introspect"
    override val tokenExchangeEndpoint = "http://localhost:7164/api/v1/token/exchange"

    override val saksbehandlingApiAudience = "tiltakspenger-saksbehandling-api"
    override val saksbehandlingApiUrl = "http://localhost:8080"

    override val dokarkivScope = "localhost"
    override val dokarkivUrl = WIREMOCK_URL

    override val pdfgenrsUrl = "http://host.docker.internal:8084"

    override val arenaMeldekortServiceAudience = "meldekortservice"
    override val arenaMeldekortServiceUrl = WIREMOCK_URL

    override val meldekortFrontendUrl = "http://localhost:2223/tiltakspenger/meldekort"

    override val varselHendelseTopic = "min-side.aapen-brukervarsel-v1"
    override val microfrontendTopic = "min-side.aapen-microfrontend-v1"
    override val identhendelseTopic = "tpts.identhendelse-v1"
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

    override val saksbehandlingApiAudience = "dev-gcp:tpts:tiltakspenger-saksbehandling-api"
    override val saksbehandlingApiUrl = "http://tiltakspenger-saksbehandling-api"

    override val dokarkivScope = "dev-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv-q2.dev-fss-pub.nais.io"

    override val pdfgenrsUrl = "http://tiltakspenger-pdfgenrs"

    override val arenaMeldekortServiceAudience = "dev-fss:meldekort:meldekortservice-q2"
    override val arenaMeldekortServiceUrl = "https://meldekortservice-q2.dev-fss-pub.nais.io"

    override val meldekortFrontendUrl = "https://www.ansatt.dev.nav.no/tiltakspenger/meldekort"

    override val varselHendelseTopic = "min-side.aapen-brukervarsel-v1"
    override val microfrontendTopic = "min-side.aapen-microfrontend-v1"
    override val identhendelseTopic = "tpts.identhendelse-v1"
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

    override val saksbehandlingApiAudience = "prod-gcp:tpts:tiltakspenger-saksbehandling-api"
    override val saksbehandlingApiUrl = "http://tiltakspenger-saksbehandling-api"

    override val dokarkivScope = "prod-fss:teamdokumenthandtering:dokarkiv"
    override val dokarkivUrl = "https://dokarkiv.prod-fss-pub.nais.io"

    override val pdfgenrsUrl = "http://tiltakspenger-pdfgenrs"

    override val arenaMeldekortServiceAudience = "prod-fss:meldekort:meldekortservice"
    override val arenaMeldekortServiceUrl = "https://meldekortservice.prod-fss-pub.nais.io"

    override val meldekortFrontendUrl = "https://www.nav.no/tiltakspenger/meldekort"

    override val varselHendelseTopic = "min-side.aapen-brukervarsel-v1"
    override val microfrontendTopic = "min-side.aapen-microfrontend-v1"
    override val identhendelseTopic = "tpts.identhendelse-v1"
}

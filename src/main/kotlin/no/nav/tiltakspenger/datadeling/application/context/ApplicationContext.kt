package no.nav.tiltakspenger.datadeling.application.context

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.application.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.behandling.datadeling.BehandlingService
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.db.PostgresBehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.motta.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.client.arena.ArenaHttpClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaClient
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseConsumer
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.ArenaMeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.db.GodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.PostgresGodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.PostgresMeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.sak.db.PostgresSakRepo
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.ArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.jobber.SendTilOboService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.kafka.OboYtelserKafkaProducer
import no.nav.tiltakspenger.datadeling.vedtak.db.PostgresVedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.motta.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import java.time.Clock
import javax.sql.DataSource

open class ApplicationContext(
    open val clock: Clock,
) {
    private val log: KLogger = KotlinLogging.logger { }
    open val texasClient: TexasClient by lazy {
        TexasHttpClient(
            introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
            tokenUrl = Configuration.naisTokenEndpoint,
            tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
            clock = clock,
        )
    }

    open val dataSource: DataSource by lazy { DataSourceSetup.createDatasource(Configuration.jdbcUrl) }
    open val sessionCounter: SessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val arenaClient: ArenaClient by lazy {
        ArenaHttpClient(
            baseUrl = Configuration.arenaUrl,
            getToken = {
                texasClient.getSystemToken(
                    Configuration.arenaScope,
                    IdentityProvider.AZUREAD,
                    rewriteAudienceTarget = false,
                )
            },
        )
    }

    open val behandlingRepo: BehandlingRepo by lazy { PostgresBehandlingRepo(sessionFactory as PostgresSessionFactory) }
    open val vedtakRepo: VedtakRepo by lazy { PostgresVedtakRepo(sessionFactory as PostgresSessionFactory) }
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy { PostgresMeldeperiodeRepo(sessionFactory as PostgresSessionFactory) }
    open val godkjentMeldekortRepo: GodkjentMeldekortRepo by lazy { PostgresGodkjentMeldekortRepo(sessionFactory as PostgresSessionFactory) }
    open val sakRepo: SakRepo by lazy { PostgresSakRepo(sessionFactory as PostgresSessionFactory) }

    open val arenaMeldekortService: ArenaMeldekortService by lazy { ArenaMeldekortService(arenaClient) }
    open val arenaUtbetalingshistorikkService: ArenaUtbetalingshistorikkService by lazy {
        ArenaUtbetalingshistorikkService(
            arenaClient,
        )
    }
    open val vedtakService: VedtakService by lazy { VedtakService(vedtakRepo, arenaClient, sakRepo, clock) }
    open val behandlingService: BehandlingService by lazy { BehandlingService(behandlingRepo) }
    open val meldekortService: MeldekortService by lazy { MeldekortService(meldeperiodeRepo) }

    open val mottaNyttVedtakService: MottaNyttVedtakService by lazy { MottaNyttVedtakService(vedtakRepo) }
    open val mottaNyBehandlingService: MottaNyBehandlingService by lazy { MottaNyBehandlingService(behandlingRepo) }

    open val identhendelseService: IdenthendelseService by lazy { IdenthendelseService(sakRepo) }
    open val identhendelseConsumer: IdenthendelseConsumer by lazy {
        IdenthendelseConsumer(
            identhendelseService = identhendelseService,
            topic = Configuration.identhendelseTopic,
        )
    }

    open val oboYtelserKafkaProducer: OboYtelserKafkaProducer by lazy {
        OboYtelserKafkaProducer(
            kafkaProducer = Producer(
                KafkaConfigImpl(),
                kanLoggeKey = false,
            ),
            topic = Configuration.oboYtelserTopic,
        )
    }
    open val sendTilOboService: SendTilOboService by lazy { SendTilOboService(vedtakRepo, oboYtelserKafkaProducer) }
}

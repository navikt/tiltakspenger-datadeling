package no.nav.tiltakspenger.datadeling.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.infra.ArenaHttpClient
import no.nav.tiltakspenger.datadeling.behandling.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.behandling.infra.BehandlingPostgresRepo
import no.nav.tiltakspenger.datadeling.behandling.infra.BehandlingService
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.datadeling.identhendelse.infra.IdenthendelseConsumer
import no.nav.tiltakspenger.datadeling.infra.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.ArenaMeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.infra.GodkjentMeldekortPostgresRepo
import no.nav.tiltakspenger.datadeling.meldekort.infra.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.infra.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.datadeling.sak.infra.SakPostgresRepo
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.ArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.datadeling.vedtak.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.infra.SendTilOboService
import no.nav.tiltakspenger.datadeling.vedtak.infra.VedtakPostgresRepo
import no.nav.tiltakspenger.datadeling.vedtak.infra.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.infra.kafka.OboYtelserKafkaProducer
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

    open val behandlingRepo: BehandlingRepo by lazy { BehandlingPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val vedtakRepo: VedtakRepo by lazy { VedtakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy { MeldeperiodePostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val godkjentMeldekortRepo: GodkjentMeldekortRepo by lazy { GodkjentMeldekortPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val sakRepo: SakRepo by lazy { SakPostgresRepo(sessionFactory as PostgresSessionFactory) }

    open val arenaMeldekortService: ArenaMeldekortService by lazy { ArenaMeldekortService(arenaClient) }
    open val arenaUtbetalingshistorikkService: ArenaUtbetalingshistorikkService by lazy {
        ArenaUtbetalingshistorikkService(
            arenaClient,
        )
    }
    open val vedtakService: VedtakService by lazy { VedtakService(vedtakRepo, arenaClient, sakRepo) }
    open val behandlingService: BehandlingService by lazy { BehandlingService(behandlingRepo) }
    open val meldekortService: MeldekortService by lazy { MeldekortService(meldeperiodeRepo) }

    open val mottaNyttVedtakService: MottaNyttVedtakService by lazy { MottaNyttVedtakService(vedtakRepo, sakRepo) }
    open val mottaNyBehandlingService: MottaNyBehandlingService by lazy { MottaNyBehandlingService(behandlingRepo, sakRepo) }

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

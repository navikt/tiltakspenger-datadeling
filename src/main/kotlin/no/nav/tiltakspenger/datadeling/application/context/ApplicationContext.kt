package no.nav.tiltakspenger.datadeling.application.context

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.application.db.DataSourceSetup
import no.nav.tiltakspenger.datadeling.behandling.datadeling.BehandlingService
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.motta.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseConsumer
import no.nav.tiltakspenger.datadeling.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.MeldekortService
import no.nav.tiltakspenger.datadeling.meldekort.db.GodkjentMeldekortRepo
import no.nav.tiltakspenger.datadeling.meldekort.db.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.jobber.SendTilOboService
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.kafka.OboYtelserKafkaProducer
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.motta.MottaNyttVedtakService
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import java.time.Clock

class ApplicationContext(
    log: KLogger,
    val clock: Clock,
) {
    val texasClient: TexasClient = TexasHttpClient(
        introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
        tokenUrl = Configuration.naisTokenEndpoint,
        tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
    )

    val dataSource = DataSourceSetup.createDatasource(Configuration.jdbcUrl)
    val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)

    val arenaClient = ArenaClient(
        baseUrl = Configuration.arenaUrl,
        getToken = {
            texasClient.getSystemToken(
                Configuration.arenaScope,
                IdentityProvider.AZUREAD,
                rewriteAudienceTarget = false,
            )
        },
    )

    val behandlingRepo = BehandlingRepo(sessionFactory)
    val vedtakRepo = VedtakRepo(sessionFactory)
    val meldeperiodeRepo = MeldeperiodeRepo(sessionFactory)
    val godkjentMeldekortRepo = GodkjentMeldekortRepo(sessionFactory)
    val sakRepo = SakRepo(sessionFactory)

    val vedtakService = VedtakService(vedtakRepo, arenaClient)
    val behandlingService = BehandlingService(behandlingRepo)
    val meldekortService = MeldekortService(meldeperiodeRepo)

    val mottaNyttVedtakService = MottaNyttVedtakService(vedtakRepo)
    val mottaNyBehandlingService = MottaNyBehandlingService(behandlingRepo)

    val identhendelseService = IdenthendelseService(behandlingRepo, vedtakRepo, meldeperiodeRepo, godkjentMeldekortRepo, sakRepo)
    val identhendelseConsumer = IdenthendelseConsumer(
        identhendelseService = identhendelseService,
        topic = Configuration.identhendelseTopic,
    )

    val oboYtelserKafkaProducer = OboYtelserKafkaProducer(
        kafkaProducer = Producer(
            KafkaConfigImpl(),
            kanLoggeKey = false,
        ),
        topic = Configuration.oboYtelserTopic,
    )
    val sendTilOboService = SendTilOboService(vedtakRepo, oboYtelserKafkaProducer)
}

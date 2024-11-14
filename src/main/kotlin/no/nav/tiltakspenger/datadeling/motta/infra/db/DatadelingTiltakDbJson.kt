package no.nav.tiltakspenger.datadeling.motta.infra.db

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize

private data class TiltaksdeltagelseDbJson(
    val tiltaksnavn: String,
    val eksternTiltaksdeltakerId: String,
    val eksternGjennomføringId: String?,
)

internal fun TiltakspengerBehandling.Tiltaksdeltagelse.toDbJson(): String {
    return TiltaksdeltagelseDbJson(
        tiltaksnavn = this.tiltaksnavn,
        eksternTiltaksdeltakerId = this.eksternTiltaksdeltakerId,
        eksternGjennomføringId = this.eksternGjennomføringId,
    ).let { serialize(it) }
}

internal fun String.toTiltaksdeltagelse(): TiltakspengerBehandling.Tiltaksdeltagelse {
    val dbJson = deserialize<TiltaksdeltagelseDbJson>(this)
    return TiltakspengerBehandling.Tiltaksdeltagelse(
        tiltaksnavn = dbJson.tiltaksnavn,
        eksternTiltaksdeltakerId = dbJson.eksternTiltaksdeltakerId,
        eksternGjennomføringId = dbJson.eksternGjennomføringId,
    )
}

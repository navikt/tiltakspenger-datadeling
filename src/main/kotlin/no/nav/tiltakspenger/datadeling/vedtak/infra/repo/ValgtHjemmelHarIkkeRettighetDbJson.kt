package no.nav.tiltakspenger.datadeling.vedtak.infra.repo

import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.json.objectMapper
import tools.jackson.module.kotlin.readValue

fun String.tilValgteHjemlerHarIkkeRettighet(): List<TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet> =
    objectMapper.readValue<List<String>>(this).map {
        when (it) {
            // TODO: Gammelt navn på RETT_TIL_DAGPENGER, som bare leses og aldri skrives.
            // Fjernes når prod er sjekket for rader med verdien, og eventuelle rader er migrert.
            "FORLENGET_VENTETID" -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.RETT_TIL_DAGPENGER

            else -> TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.valueOf(it)
        }
    }

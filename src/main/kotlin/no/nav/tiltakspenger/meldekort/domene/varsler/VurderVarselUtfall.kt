package no.nav.tiltakspenger.meldekort.domene.varsler

internal sealed interface VurderVarselUtfall {
    data object HarPågåendeInaktivering : VurderVarselUtfall

    data object KanIkkeErstattePåGrunnAvCooldown : VurderVarselUtfall
}

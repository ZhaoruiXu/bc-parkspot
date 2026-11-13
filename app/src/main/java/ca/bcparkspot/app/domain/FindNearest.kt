package ca.bcparkspot.app.domain

data class LotCandidate(
    val lotId: String,
    val parkId: String,
    val driveMinutes: Int,
    val state: LotState,
    /** What sources say about the lot when the driver would arrive, if anything. */
    val outlook: Outlook? = null
)

sealed interface NearestResult {
    data class Confirmed(val candidate: LotCandidate) : NearestResult
    data class Fallback(val candidate: LotCandidate) : NearestResult
    data object NothingQualifies : NearestResult
}

private val nearestOrder: Comparator<LotCandidate> =
    compareBy<LotCandidate> { it.driveMinutes }
        .thenByDescending { it.state.reporterCount }
        .thenBy { it.state.newestAgeMs ?: Long.MAX_VALUE }
        .thenBy { it.lotId }

private fun LotCandidate.isOpenSideUnconfirmed(): Boolean =
    state is LotState.Unconfirmed && sideOf(state.newestStatus) == ReportSide.OPEN

private fun LotCandidate.isUnreported(): Boolean = state == LotState.NoRecentReports

private fun List<LotCandidate>.nearest(predicate: (LotCandidate) -> Boolean): LotCandidate? =
    filter(predicate).minWithOrNull(nearestOrder)

private val fallbackTiers: List<(LotCandidate) -> Boolean> = listOf(
    { it.isOpenSideUnconfirmed() },
    { it.isUnreported() && it.outlook !in BUSY_OUTLOOKS },
    { it.isUnreported() }
)

/**
 * A confirmed open lot first, in the target park and then anywhere. Failing that, the best fallback in the target
 * park, then anywhere: a lone open-side report, an unreported lot no source calls busy then, and an unreported lot
 * a source calls busy then. A lot that is closed, or whose gate is usually shut when the driver
 * would arrive, is never offered, whatever its reports say.
 */
fun findNearestOpenSpot(candidates: List<LotCandidate>, targetParkId: String? = null): NearestResult {
    val reachable = candidates.filter { it.outlook !in BLOCKING_OUTLOOKS }
    val inScope = if (targetParkId == null) reachable else reachable.filter { it.parkId == targetParkId }
    val scopes = listOf(inScope, reachable).distinct()
    scopes.firstNotNullOfOrNull { scope -> scope.nearest { it.state.isConfirmedOpen() } }
        ?.let { return NearestResult.Confirmed(it) }
    scopes.firstNotNullOfOrNull { scope -> fallbackTiers.firstNotNullOfOrNull { tier -> scope.nearest(tier) } }
        ?.let { return NearestResult.Fallback(it) }
    return NearestResult.NothingQualifies
}

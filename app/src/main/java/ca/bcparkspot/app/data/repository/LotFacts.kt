package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.domain.TodayAtLot
import ca.bcparkspot.app.domain.LotFacts
import ca.bcparkspot.app.domain.isOpenAt
import ca.bcparkspot.app.domain.mayBeClosedAt
import ca.bcparkspot.app.domain.today
import ca.bcparkspot.app.domain.Prediction
import ca.bcparkspot.app.domain.predictOutlook
import ca.bcparkspot.app.model.ParkingLot
import java.time.LocalDateTime

/** What the bundled data says about this lot at [at], in BC local time: busy advice, gate hours, closures and passes. */
fun ParkingLot.factsAt(at: LocalDateTime): LotFacts = LotFacts(
    advice = FillPatterns.BY_LOT[id],
    gate = GateWindows.BY_LOT[id],
    parkClosed = getActiveClosure(at.toLocalDate()) != null,
    passRequired = isPassRequiredToday(at.toLocalDate(), at.toLocalTime())
)

fun ParkingLot.predictionAt(at: LocalDateTime): Prediction? = predictOutlook(factsAt(at), at)

/** The published facts for this lot at [at] in BC time, for visitors before anyone has reported on it. */
fun ParkingLot.todayAt(at: LocalDateTime): TodayAtLot {
    val gate = GateWindows.BY_LOT[id]
    return TodayAtLot(
        gate = gate.today(at.toLocalDate()),
        pass = getPassState(at.toLocalDate(), at.toLocalTime()),
        gateOpenNow = gate.isOpenAt(at) && !gate.mayBeClosedAt(at)
    )
}

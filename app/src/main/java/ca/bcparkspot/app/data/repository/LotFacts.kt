package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.domain.LotFacts
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

package com.emilio.fitarchive.data

import com.emilio.fitarchive.db.FitArchiveDatabase
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

/** Punto de entrada a los datos locales; las pantallas usan solo las interfaces. */
class Repositories(
    db: FitArchiveDatabase,
    context: CoroutineContext = Dispatchers.Default,
    now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    val garments: GarmentRepository = SqlGarmentRepository(db, context)
    val outfits: OutfitRepository = SqlOutfitRepository(db, context)
    val measurements: MeasurementsRepository = SqlMeasurementsRepository(db, context, now)
    val calendar: CalendarRepository = SqlCalendarRepository(db, context)
    val trips: TripRepository = SqlTripRepository(db, context)
}

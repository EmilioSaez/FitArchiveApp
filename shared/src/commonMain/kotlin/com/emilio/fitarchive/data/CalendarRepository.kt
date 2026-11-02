package com.emilio.fitarchive.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emilio.fitarchive.db.FitArchiveDatabase
import com.emilio.fitarchive.model.CalendarDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Las fechas son ISO-8601 (yyyy-MM-dd). */
interface CalendarRepository {
    fun observe(date: String): Flow<CalendarDay?>

    /** Días con registro entre [from] y [to], ambos inclusive, ordenados por fecha. */
    fun observeBetween(from: String, to: String): Flow<List<CalendarDay>>

    /** Guarda el día, reemplazando el que hubiera en esa fecha. */
    suspend fun set(day: CalendarDay)
    suspend fun clear(date: String)
}

internal class SqlCalendarRepository(
    db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : CalendarRepository {
    private val queries = db.calendarDayQueries

    override fun observe(date: String) =
        queries.selectByDate(date).asFlow().mapToOneOrNull(context).map { it?.toModel() }

    override fun observeBetween(from: String, to: String) =
        queries.selectBetween(from, to).asFlow().mapToList(context).map { it.map { d -> d.toModel() } }

    override suspend fun set(day: CalendarDay) = withContext(context) {
        queries.upsert(day.date, day.outfitId, day.note)
        Unit
    }

    override suspend fun clear(date: String) = withContext(context) { queries.deleteByDate(date); Unit }
}

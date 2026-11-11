package com.auza.giguclone.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.auza.giguclone.domain.model.DailyStatistics
import com.auza.giguclone.domain.model.EvaluationResult
import com.auza.giguclone.domain.model.PlatformType
import com.auza.giguclone.domain.model.TrafficLightColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RideDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "gigu_rides.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_RIDES = "rides_history"
        const val COL_ID = "id"
        const val COL_PLATFORM = "platform"
        const val COL_FARE = "fare"
        const val COL_TOTAL_KM = "total_km"
        const val COL_TIME_MIN = "time_min"
        const val COL_RATE_KM = "rate_km"
        const val COL_RATE_HOUR = "rate_hour"
        const val COL_TOTAL_COST = "total_cost"
        const val COL_NET_PROFIT = "net_profit"
        const val COL_COLOR = "color"
        const val COL_IS_RISK = "is_risk"
        const val COL_PICKUP = "pickup"
        const val COL_DESTINATION = "destination"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_DATE_STR = "date_str"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_RIDES (
                $COL_ID TEXT PRIMARY KEY,
                $COL_PLATFORM TEXT,
                $COL_FARE REAL,
                $COL_TOTAL_KM REAL,
                $COL_TIME_MIN INTEGER,
                $COL_RATE_KM REAL,
                $COL_RATE_HOUR REAL,
                $COL_TOTAL_COST REAL,
                $COL_NET_PROFIT REAL,
                $COL_COLOR TEXT,
                $COL_IS_RISK INTEGER,
                $COL_PICKUP TEXT,
                $COL_DESTINATION TEXT,
                $COL_TIMESTAMP INTEGER,
                $COL_DATE_STR TEXT
            )
        """.trimIndent()
        db.execSQL(createQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RIDES")
        onCreate(db)
    }

    fun insertEvaluation(evaluation: EvaluationResult) {
        val db = writableDatabase
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(evaluation.offer.timestamp))

        val values = ContentValues().apply {
            put(COL_ID, evaluation.offer.id)
            put(COL_PLATFORM, evaluation.offer.platform.name)
            put(COL_FARE, evaluation.offer.fare)
            put(COL_TOTAL_KM, evaluation.offer.totalDistanceKm)
            put(COL_TIME_MIN, evaluation.offer.totalTimeMinutes)
            put(COL_RATE_KM, evaluation.ratePerKm)
            put(COL_RATE_HOUR, evaluation.ratePerHour)
            put(COL_TOTAL_COST, evaluation.totalCost)
            put(COL_NET_PROFIT, evaluation.netProfit)
            put(COL_COLOR, evaluation.color.name)
            put(COL_IS_RISK, if (evaluation.isRiskArea || evaluation.isLowRating) 1 else 0)
            put(COL_PICKUP, evaluation.offer.pickupAddress ?: "")
            put(COL_DESTINATION, evaluation.offer.destinationAddress ?: "")
            put(COL_TIMESTAMP, evaluation.offer.timestamp)
            put(COL_DATE_STR, dateStr)
        }

        db.insertWithOnConflict(TABLE_RIDES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getTodayStatistics(): DailyStatistics {
        val db = readableDatabase
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val cursor = db.rawQuery(
            "SELECT $COL_COLOR, $COL_FARE, $COL_NET_PROFIT, $COL_TOTAL_KM, $COL_TIME_MIN, $COL_IS_RISK FROM $TABLE_RIDES WHERE $COL_DATE_STR = ?",
            arrayOf(todayStr)
        )

        var totalOffers = 0
        var greenCount = 0
        var yellowCount = 0
        var redCount = 0
        var riskCount = 0
        var totalGross = 0.0
        var totalNet = 0.0
        var totalKm = 0.0
        var totalMinutes = 0

        if (cursor.moveToFirst()) {
            do {
                totalOffers++
                val colorStr = cursor.getString(0)
                val fare = cursor.getDouble(1)
                val net = cursor.getDouble(2)
                val km = cursor.getDouble(3)
                val minutes = cursor.getInt(4)
                val isRisk = cursor.getInt(5) == 1

                when (colorStr) {
                    TrafficLightColor.GREEN.name -> greenCount++
                    TrafficLightColor.YELLOW.name -> yellowCount++
                    TrafficLightColor.RED.name -> redCount++
                    TrafficLightColor.PURPLE_RISK.name -> riskCount++
                }

                if (isRisk && colorStr != TrafficLightColor.PURPLE_RISK.name) riskCount++

                totalGross += fare
                totalNet += net
                totalKm += km
                totalMinutes += minutes
            } while (cursor.moveToNext())
        }
        cursor.close()

        return DailyStatistics(
            totalOffers = totalOffers,
            greenCount = greenCount,
            yellowCount = yellowCount,
            redCount = redCount,
            riskCount = riskCount,
            totalGrossFare = totalGross,
            totalNetProfit = totalNet,
            totalKmDriven = totalKm,
            totalTimeMinutes = totalMinutes
        )
    }

    fun getRecentHistory(limit: Int = 40): List<EvaluationResult> {
        val db = readableDatabase
        val list = mutableListOf<EvaluationResult>()
        val cursor = db.query(
            TABLE_RIDES,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID))
                val platformName = cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM))
                val fare = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_FARE))
                val totalKm = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TOTAL_KM))
                val timeMin = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TIME_MIN))
                val rateKm = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RATE_KM))
                val rateHour = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RATE_HOUR))
                val totalCost = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TOTAL_COST))
                val netProfit = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_NET_PROFIT))
                val colorStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_COLOR))
                val isRisk = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_RISK)) == 1
                val pickup = cursor.getString(cursor.getColumnIndexOrThrow(COL_PICKUP))
                val dest = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESTINATION))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))

                val platform = try { PlatformType.valueOf(platformName) } catch (e: Exception) { PlatformType.UNKNOWN }
                val color = try { TrafficLightColor.valueOf(colorStr) } catch (e: Exception) { TrafficLightColor.YELLOW }
                val profitPct = if (fare > 0) (netProfit / fare) * 100.0 else 0.0

                val offer = com.auza.giguclone.domain.model.RideOffer(
                    id = id,
                    platform = platform,
                    fare = fare,
                    pickupDistanceKm = 1.0,
                    tripDistanceKm = if (totalKm > 1.0) totalKm - 1.0 else totalKm,
                    pickupTimeMinutes = 2,
                    tripTimeMinutes = if (timeMin > 2) timeMin - 2 else timeMin,
                    pickupAddress = pickup,
                    destinationAddress = dest,
                    timestamp = timestamp
                )

                list.add(
                    EvaluationResult(
                        offer = offer,
                        totalCost = totalCost,
                        netProfit = netProfit,
                        profitPercentage = profitPct,
                        ratePerKm = rateKm,
                        ratePerHour = rateHour,
                        color = color,
                        isRiskArea = isRisk,
                        verdictSummary = "Histórico salvo"
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}

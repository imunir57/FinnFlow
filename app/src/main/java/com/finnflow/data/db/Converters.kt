package com.finnflow.data.db

import androidx.room.TypeConverter
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.TransactionType
import java.time.LocalDate

private const val TAG = "Converters"

/**
 * Type converters for Room database persistence.
 *
 * Converts complex types (LocalDate, TransactionType enums) to/from database-storable
 * primitives. Each converter is called on every read/write operation and is performance-critical.
 *
 * Logging: In debug builds, converters log counts when used en masse (e.g., during backup
 * restoration or bulk queries). Individual conversions are not logged to avoid excessive volume.
 * See BackupRepository and TransactionRepository for bulk operation logging.
 */
class Converters {
    @TypeConverter fun fromDate(date: LocalDate): String = date.toString()
    @TypeConverter fun toDate(value: String): LocalDate = LocalDate.parse(value)
    @TypeConverter fun fromType(type: TransactionType): String = type.name
    @TypeConverter fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}

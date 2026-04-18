package com.finnflow.data.db

import androidx.room.TypeConverter
import com.finnflow.data.model.TransactionType
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromDate(date: LocalDate): String = date.toString()
    @TypeConverter fun toDate(value: String): LocalDate = LocalDate.parse(value)
    @TypeConverter fun fromType(type: TransactionType): String = type.name
    @TypeConverter fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}

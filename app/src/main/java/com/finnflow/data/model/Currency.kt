package com.finnflow.data.model

/**
 * Supported currencies for the user's profile. [code] is the ISO 4217 code
 * persisted in DataStore.
 */
enum class Currency(val code: String, val symbol: String, val displayName: String) {
    BDT("BDT", "৳", "Bangladeshi Taka"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    INR("INR", "₹", "Indian Rupee");

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.code == code } ?: BDT
    }
}

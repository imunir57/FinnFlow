package com.finnflow.data.db

import com.finnflow.data.model.TransactionType

/**
 * Defines every default category and its subcategories that get inserted on first launch.
 * Colors use Material-friendly hex values.
 */
object SeedData {

    data class SeedCategory(
        val name: String,
        val type: TransactionType,
        val iconName: String,
        val colorHex: String,
        val subCategories: List<String> = emptyList()
    )

    val categories: List<SeedCategory> = listOf(

        // ── EXPENSE ──────────────────────────────────────────────────────
        SeedCategory(
            name = "Food & Dining",
            type = TransactionType.EXPENSE,
            iconName = "ic_food",
            colorHex = "#F44336",
            subCategories = listOf("Restaurant", "Groceries", "Coffee", "Fast Food", "Bakery", "Street Food")
        ),
        SeedCategory(
            name = "Transportation",
            type = TransactionType.EXPENSE,
            iconName = "ic_transport",
            colorHex = "#2196F3",
            subCategories = listOf("Bus", "Train", "CNG / Rickshaw", "Ride Share", "Fuel", "Parking", "Taxi")
        ),
        SeedCategory(
            name = "Housing",
            type = TransactionType.EXPENSE,
            iconName = "ic_home",
            colorHex = "#795548",
            subCategories = listOf("Rent", "Electricity", "Water", "Gas", "Internet", "Maintenance", "Furniture")
        ),
        SeedCategory(
            name = "Health",
            type = TransactionType.EXPENSE,
            iconName = "ic_health",
            colorHex = "#E91E63",
            subCategories = listOf("Doctor", "Medicine", "Hospital", "Lab Tests", "Dental", "Pharmacy", "Gym")
        ),
        SeedCategory(
            name = "Education",
            type = TransactionType.EXPENSE,
            iconName = "ic_education",
            colorHex = "#9C27B0",
            subCategories = listOf("Tuition", "Books", "Stationery", "Coaching", "Online Course", "Exam Fee")
        ),
        SeedCategory(
            name = "Shopping",
            type = TransactionType.EXPENSE,
            iconName = "ic_shopping",
            colorHex = "#FF9800",
            subCategories = listOf("Clothing", "Electronics", "Footwear", "Accessories", "Home Goods", "Beauty")
        ),
        SeedCategory(
            name = "Entertainment",
            type = TransactionType.EXPENSE,
            iconName = "ic_entertainment",
            colorHex = "#00BCD4",
            subCategories = listOf("Movies", "Streaming", "Games", "Books", "Music", "Sports", "Events")
        ),
        SeedCategory(
            name = "Communication",
            type = TransactionType.EXPENSE,
            iconName = "ic_phone",
            colorHex = "#607D8B",
            subCategories = listOf("Mobile Recharge", "Internet Pack", "Phone Bill")
        ),
        SeedCategory(
            name = "Personal Care",
            type = TransactionType.EXPENSE,
            iconName = "ic_personal",
            colorHex = "#FF5722",
            subCategories = listOf("Haircut", "Grooming", "Toiletries", "Cosmetics")
        ),
        SeedCategory(
            name = "Family & Gifts",
            type = TransactionType.EXPENSE,
            iconName = "ic_gift",
            colorHex = "#8BC34A",
            subCategories = listOf("Gifts", "Donations", "Family Support", "Charity")
        ),
        SeedCategory(
            name = "Finance",
            type = TransactionType.EXPENSE,
            iconName = "ic_finance",
            colorHex = "#3F51B5",
            subCategories = listOf("Loan Repayment", "Insurance", "Bank Charges", "Tax", "Investment")
        ),
        SeedCategory(
            name = "Other Expense",
            type = TransactionType.EXPENSE,
            iconName = "ic_other",
            colorHex = "#9E9E9E",
            subCategories = listOf("Miscellaneous")
        ),

        // ── INCOME ───────────────────────────────────────────────────────
        SeedCategory(
            name = "Salary",
            type = TransactionType.INCOME,
            iconName = "ic_salary",
            colorHex = "#4CAF50",
            subCategories = listOf("Monthly Salary", "Bonus", "Overtime", "Allowance")
        ),
        SeedCategory(
            name = "Business",
            type = TransactionType.INCOME,
            iconName = "ic_business",
            colorHex = "#009688",
            subCategories = listOf("Sales", "Service Revenue", "Commission", "Consulting")
        ),
        SeedCategory(
            name = "Freelance",
            type = TransactionType.INCOME,
            iconName = "ic_freelance",
            colorHex = "#00BCD4",
            subCategories = listOf("Project Payment", "Part-time Work", "Online Work")
        ),
        SeedCategory(
            name = "Investment Returns",
            type = TransactionType.INCOME,
            iconName = "ic_investment",
            colorHex = "#8BC34A",
            subCategories = listOf("Dividends", "Interest", "Capital Gains", "Rental Income")
        ),
        SeedCategory(
            name = "Other Income",
            type = TransactionType.INCOME,
            iconName = "ic_other_income",
            colorHex = "#9E9E9E",
            subCategories = listOf("Gift Received", "Refund", "Miscellaneous")
        ),

        // ── TRANSFER ─────────────────────────────────────────────────────
        SeedCategory(
            name = "Account Transfer",
            type = TransactionType.TRANSFER,
            iconName = "ic_transfer",
            colorHex = "#FF9800",
            subCategories = listOf("Bank to Cash", "Cash to Bank", "Between Accounts")
        )
    )
}

package org.example.banca.util

private const val BLZ = "12030000"

object IbanGenerator {
    private const val RANGE_END = 999_999_999_9
    private const val RANGE_START = 100_000_000_0

    fun generate(): String {
        val accountNumber = (RANGE_START..RANGE_END).random().toString()
        val checksum = (10..99).random().toString()
        return "DE$checksum$BLZ$accountNumber"
    }
}

fun isValidIban(iban: String): Boolean = when (iban.length) {
    27 -> iban.startsWith("FR") || iban.startsWith("IT")
    22 -> iban.startsWith("DE")
    16 -> iban.startsWith("BE")
    else -> false
}

fun isValidBic(bic: String, iban: String): Boolean {
    return bicBlzMap[findBlz(iban)] == bic
}

fun findBlz(iban: String): String = when {
    iban.startsWith("DE") -> iban.substring(4..11)
    iban.startsWith("IT") -> iban.substring(5..9)
    iban.startsWith("FR") -> iban.substring(4..8)
    iban.startsWith("BE") -> iban.substring(4..6)
    else -> "unknown"
}

fun isInternalIban(iban: String): Boolean = isValidIban(iban) && findBlz(iban) == BLZ

private val bicBlzMap = mapOf(
    "BYLADEM1001" to "12030000",
    "SOGEFRPPXXX" to "11010101",
    "UNCRITMMXXX" to "02008",
    "SOGEFRPPXXX" to "30003",
    "TRWIBEB1" to "967"
)

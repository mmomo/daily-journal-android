package com.mmomo.dailyjournal.domain.time

import kotlin.math.absoluteValue

/**
 * Deterministic hashing, identical to iOS, so prompt picks match across platforms (BR-P2).
 * Kotlin's `hashCode()` must never be used for prompt selection.
 */
object StableHash {
    private const val OFFSET_BASIS = 0xcbf29ce484222325uL
    private const val PRIME = 0x100000001b3uL

    /** FNV-1a 64-bit over the UTF-8 bytes of [text]. */
    fun fnv1a64(text: String): ULong {
        var hash = OFFSET_BASIS
        for (byte in text.encodeToByteArray()) {
            hash = hash xor byte.toUByte().toULong()
            hash *= PRIME
        }
        return hash
    }

    fun gcd(a: Int, b: Int): Int {
        var x = a.absoluteValue
        var y = b.absoluteValue
        while (y != 0) {
            val r = x % y
            x = y
            y = r
        }
        return x
    }
}

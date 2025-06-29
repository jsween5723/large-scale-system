package io.sween.largescalesystem.ratelimit

enum class UserLimitation(val limit: Int) {
    LOW(2),
    MEDIUM(3),
    HIGH(4),
}
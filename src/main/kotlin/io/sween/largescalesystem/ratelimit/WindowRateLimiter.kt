package io.sween.largescalesystem.ratelimit

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Component
class WindowRateLimiter(private val redisTemplate: RedisTemplate<String, String>) {
    @Transactional
    fun <T> rateLimit(key: Any, windowSecs: Long, limitCount: Long, operation: () -> T): T {
        val now = Instant.now()
        val nowNano = now.toEpochMilli() * 1_000_000 + now.nano
        val rawKey = redisTemplate.stringSerializer.serialize(key.toString())!!
        val windowSecsNano = windowSecs * 1_000_000_000
        val result = redisTemplate.execute {
            it.multi()
            it.zSetCommands()
                .zRemRangeByScore(rawKey, 0.0, (nowNano - windowSecsNano).toDouble())
            it.zAdd(rawKey,
                nowNano.toDouble(),
                redisTemplate.stringSerializer.serialize(nowNano.toString())!!)
            it.zSetCommands()
                .zCard(rawKey)
            it.keyCommands()
                .expire(rawKey, windowSecs)
            it.exec()
        } ?: throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)
        val count =
            result[2] as? Long ?: throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)
        if (count > limitCount) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)
        }
        return operation()
    }
}
package io.sween.largescalesystem.ratelimit

import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/rate-limit")
class RateLimitController(private val rateLimiter: WindowRateLimiter) {
    @GetMapping("nginx")
    fun getRateLimit(): String {
        return "success"
    }
    @GetMapping("bucket")
    fun getBucket(): String {
        return "success"
    }

    @GetMapping("resilience4j")
    @RateLimiter(name = "myRateLimitedService", fallbackMethod = "fallback")
    fun getResilience4j(): String {
        return "success"
    }

    @GetMapping("/redis")
    fun redis(): String = rateLimiter.rateLimit("key", 500, 3) {
        "success"
    }

    fun fallback(e: Throwable): String {
        throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)
    }
}
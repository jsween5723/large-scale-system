package io.sween.largescalesystem.ratelimit

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rate-limit")
class RateLimitController {
    @GetMapping("nginx")
    fun getRateLimit(): String {
        return "success"
    }
    @GetMapping("bucket")
    fun getBucket(): String {
        return "success"
    }
}
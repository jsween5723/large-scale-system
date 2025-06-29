package io.sween.largescalesystem.ratelimit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.concurrent.CompletableFuture


@SpringBootTest
@AutoConfigureMockMvc
@Import(EmbeddedRedisConfig::class)
class RedisWindowTest : RedisServerSuper() {
    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun window() {
        val successResult = IntRange(1, 3).map { CompletableFuture.supplyAsync { sendRequest() } }
            .toTypedArray()
        CompletableFuture.allOf(*successResult).get()
        assertThat(successResult.map { it.get() }).isEqualTo(listOf(200, 200, 200))
        val failRequest = sendRequest()
        assertThat(failRequest).isEqualTo(429)
    }

    private fun sendRequest(): Int {
        return mockMvc.get("/rate-limit/redis")
            .andReturn().response.status
    }
}


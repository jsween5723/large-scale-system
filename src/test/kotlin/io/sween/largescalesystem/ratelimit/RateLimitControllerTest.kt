package io.sween.largescalesystem.ratelimit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.util.concurrent.CompletableFuture

@SpringBootTest
class RateLimitControllerTest {
    val client: RestClient = RestClient.builder()
        .baseUrl("http://localhost:80")
        .build()

    @Test
    fun `Should get rate limit`() {
        val futures = IntRange(1, 30).map {
            CompletableFuture.supplyAsync { 요청하기() }
        }
            .toTypedArray()
        CompletableFuture.allOf(*futures)
        val statuses = futures.map {
                it.join()
        }
        val okCount = statuses.count { it == 200 }
        val tooManyRequestCount = statuses.count { it == 429 }
        assertThat(okCount).isEqualTo(11)
        assertThat(tooManyRequestCount).isEqualTo(19)
    }

    private fun 요청하기(): Int {
        return try {
            client.get()
                .uri("/rate-limit/nginx").retrieve().toBodilessEntity().statusCode.value()
        } catch (e: HttpClientErrorException) {
            e.statusCode.value()
        }
    }
}
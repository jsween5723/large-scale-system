package io.sween.largescalesystem.ratelimit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.util.concurrent.CompletableFuture

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitControllerTest {
    val client: RestClient = RestClient.builder()
        .baseUrl("http://localhost:80")
        .build()
    @Autowired
    lateinit var mockMvc: MockMvc
    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    @DisplayName("nginx")
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

    @Test
    @DisplayName("bucket 4j")
    fun `버킷4j`() {
        `when`(userRepository.findBy(1)).then { User(id = 1, limitation = UserLimitation.LOW) }
        `when`(userRepository.findBy(2)).then { User(id = 2, limitation = UserLimitation.MEDIUM) }
        `when`(userRepository.findBy(3)).then { User(id = 3, limitation = UserLimitation.HIGH) }
        val successRequest = IntRange(1, 2).map {
            CompletableFuture.supplyAsync { sendRequest(1) }
        }.toTypedArray()
        val successRequest2 = IntRange(1, 3).map {
            CompletableFuture.supplyAsync { sendRequest(2) }
        }.toTypedArray()
        val successRequest3 = IntRange(1, 4).map { CompletableFuture.supplyAsync { sendRequest(3) } }.toTypedArray()

        CompletableFuture.allOf(*successRequest, *successRequest2, *successRequest3).join()
        val failRequest = sendRequest(1)
        println(successRequest.map { it.get() })
        successRequest.forEach { assertThat(it.get()).isEqualTo(200) }
        successRequest2.forEach { assertThat(it.get()).isEqualTo(200) }
        successRequest3.forEach { assertThat(it.get()).isEqualTo(200) }
        assertThat(failRequest).isEqualTo(429)
    }

    private fun sendRequest(id: Int): Int {
        return mockMvc.get("/rate-limit/bucket") {
            header("Authorization", id.toString())
        }
            .andReturn().response.status
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
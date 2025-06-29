package io.sween.largescalesystem.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class Bucket4jInterceptor(private val userRepository: UserRepository) : RateLimitInterceptor {
    private val ipBuckets = ConcurrentHashMap<String, Bucket>()
    private val planBuckets = ConcurrentHashMap<User, Bucket>()
    override fun preHandle(request: HttpServletRequest,
                           response: HttpServletResponse,
                           handler: Any): Boolean {
//        initIpBucket(request)
//        checkIpBucket(request, response)
        initPlanBucket(request)
        checkPlanBucket(request, response)
        return super.preHandle(request, response, handler)
    }

    private fun checkPlanBucket(request: HttpServletRequest,
                          response: HttpServletResponse) {
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.let { id ->
                val user = userRepository.findBy(id.toInt())
                planBuckets[user]
                    ?.let {
                        if (!it.tryConsume(1)) {
                            responseTooMany(response)
                        }
                    }
            }
    }

    private fun checkIpBucket(request: HttpServletRequest,
                          response: HttpServletResponse) {
        ipBuckets[request.remoteAddr.toString()]
            ?.let {
                if (!it.tryConsume(1)) {
                    responseTooMany(response)
                }
            }
    }

    private fun initPlanBucket(request: HttpServletRequest) {
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.let {
                val user = userRepository.findBy(it.toInt())
                planBuckets.putIfAbsent(user, Bucket.builder()
                    .addLimit(BandwidthBuilder.builder()
                        .capacity(user.limitation.limit.toLong())
                        .refillIntervally(10, Duration.ofSeconds(1))
                        .build())
                    .build())
            }
    }

    private fun initIpBucket(request: HttpServletRequest) {
        ipBuckets.putIfAbsent(request.remoteAddr.toString(),
            Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofSeconds(1))
                    .build())
                .build())
    }

    private fun responseTooMany(response: HttpServletResponse) {
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value())
        response.flushBuffer()
    }
}
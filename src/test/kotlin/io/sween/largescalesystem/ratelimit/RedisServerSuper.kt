package io.sween.largescalesystem.ratelimit

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import redis.embedded.RedisServer
import kotlin.random.Random

open class RedisServerSuper {
    lateinit var redisServer: RedisServer
    companion object {
        val REDIS_PORT = Random.nextInt(6000, 10000)
    }
    @PostConstruct
    fun startRedis() {
        // 기본 포트 6379, 로컬에서 테스트용으로만 사용
        this.redisServer = RedisServer(REDIS_PORT.toInt())
            .also { it.start() }
    }

    @PreDestroy
    fun stopRedis() {
        redisServer.stop()
    }
}
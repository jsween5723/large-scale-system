package io.sween.largescalesystem.ratelimit

import io.sween.largescalesystem.ratelimit.RedisServerSuper.Companion.REDIS_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

@TestConfiguration
class EmbeddedRedisConfig {
    @Bean
    @Primary // 테스트 컨텍스트에 이 커넥션 팩토리가 우선 사용됨
    fun redisConnectionFactory(): LettuceConnectionFactory {
        // 호스트 localhost, 포트 6379 (위에서 띄운 Embedded Redis)
        return LettuceConnectionFactory("localhost", REDIS_PORT)
    }

    @Bean
    @Primary
    fun redisTemplate(cf: LettuceConnectionFactory): RedisTemplate<Any, Any> {
        val template = RedisTemplate<Any, Any>()
        template.connectionFactory = cf
        // 필요에 따라 key/value 시리얼라이저 설정
        // template.setKeySerializer(new StringRedisSerializer());
        // template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template
    }
}
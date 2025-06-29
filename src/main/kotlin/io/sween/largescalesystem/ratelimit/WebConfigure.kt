package io.sween.largescalesystem.ratelimit

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfigure(private val rateLimitInterceptor: RateLimitInterceptor): WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/rate-limit/bucket")
    }
}
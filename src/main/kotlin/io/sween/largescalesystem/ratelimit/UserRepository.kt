package io.sween.largescalesystem.ratelimit

import org.springframework.stereotype.Component

interface UserRepository {
    fun findBy(id: Int): User 
}

@Component
class UserRepositoryImpl() : UserRepository {
    override fun findBy(id: Int): User {
        return User(id = 6717, limitation = UserLimitation.MEDIUM)
    }
}

data class User(val id: Int, val limitation: UserLimitation)
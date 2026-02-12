package com.example.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.models.User
import com.example.repositories.UserRepository

class UserService(private val userRepository: UserRepository) {
    
    companion object {
        fun hashPassword(password: String): String {
            return BCrypt.withDefaults().hashToString(12, password.toCharArray())
        }
        
        fun verifyPassword(password: String, hash: String): Boolean {
            return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
        }
    }

    suspend fun createUser(user: User): Int {
        return userRepository.create(user)
    }

    suspend fun getUserById(id: Int): User? {
        return userRepository.findById(id)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun updateUser(id: Int, user: User, newPassword: String? = null) {
        val passwordHash = newPassword?.let { hashPassword(it) }
        userRepository.update(id, user, passwordHash)
    }

    suspend fun deleteUser(id: Int) {
        userRepository.delete(id)
    }

    suspend fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }

    suspend fun banUser(id: Int) {
        userRepository.ban(id)
    }

    suspend fun unbanUser(id: Int) {
        userRepository.unban(id)
    }
}

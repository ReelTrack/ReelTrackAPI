package com.example.services

import com.example.models.Person
import com.example.repositories.PersonRepository

class PersonService(private val personRepository: PersonRepository) {

    suspend fun createPerson(person: Person): Int {
        return personRepository.create(person)
    }

    suspend fun getPersonById(id: Int): Person? {
        return personRepository.findById(id)
    }

    suspend fun getAllPersons(search: String? = null): List<Person> {
        return personRepository.findAll(search)
    }

    suspend fun updatePerson(id: Int, person: Person) {
        personRepository.update(id, person)
    }

    suspend fun deletePerson(id: Int) {
        personRepository.delete(id)
    }
}
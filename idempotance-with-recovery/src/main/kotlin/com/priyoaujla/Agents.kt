package com.priyoaujla

import com.ptrbrynt.jsondsl.jsonObject

class SignupAgent(
        private val emailInbox: EmailInbox,
        private val storage: BusinessEventStorage
): () -> Unit {

    override fun invoke() = storage.insert(BusinessEvent(BusinessEvent.Name.Subscribed, jsonObject {
        "email" to emailInbox.address().value
    }))
}


interface EmailInbox {
    fun list(): List<Email>
    fun address(): EmailAddress
}

data class Email(val subject: String, val body: String)
data class EmailAddress(val value: String)
package com.priyoaujla

class SignupAgent(
        val id: Int,
        val emailInbox: EmailInbox
) {

}


interface EmailInbox {
    fun list(): List<Email>
}

data class Email(val subject: String, val body: String)
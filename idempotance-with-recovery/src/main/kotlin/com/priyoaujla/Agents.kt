package com.priyoaujla

import java.util.concurrent.CopyOnWriteArrayList

class MailingListSubscriber(
        private val emailInbox: EmailInbox,
        private val subscriptions: Subscriptions
){

    fun subscribe() = subscriptions.subscribe(emailInbox.address)
}

class EmailMarketer(
    private val storage: BusinessEventStorage
) {
    fun sendEmail(): Sequence<BusinessEvent> {
        return storage.all()
    }
}


interface EmailInbox {
    val address: EmailAddress
    fun list(): List<Email>
}

class ThreadSafeEmailInbox(override val address: EmailAddress) :  EmailInbox {

    private val inbox = CopyOnWriteArrayList<Email>()

    override fun list(): List<Email> {
        return inbox.toList()
    }

}

data class Email(val subject: String, val body: String)
data class EmailAddress(val value: String)
package com.priyoaujla

import com.ptrbrynt.jsondsl.jsonObject
import java.util.concurrent.CopyOnWriteArrayList

class MailingListSubscriber(
        private val emailInbox: EmailInbox,
        private val storage: BusinessEventStorage
){

    fun subscribe() = storage.insert(BusinessEvent(BusinessEvent.Name.Subscribed, jsonObject {
        "email" to emailInbox.address.value
    }))
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
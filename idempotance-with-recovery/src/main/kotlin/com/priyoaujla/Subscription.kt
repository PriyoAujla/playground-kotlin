package com.priyoaujla

import com.ptrbrynt.jsondsl.jsonObject

class Subscription(private val storage: BusinessEventStorage) {

    fun add(emailAddress: EmailAddress){
        storage.insert(BusinessEvent(BusinessEvent.Name.Subscribed, jsonObject {
            "email" to emailAddress.value
        }))
    }

    fun all(): List<EmailAddress> {

    }
}
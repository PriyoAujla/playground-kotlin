package com.priyoaujla

import com.ptrbrynt.jsondsl.jsonObject

class Subscriptions(private val storage: BusinessEventStorage) {

    fun subscribe(emailAddress: EmailAddress){
        storage.insert(BusinessEvent(BusinessEvent.Name.Subscribed, jsonObject {
            "email" to emailAddress.value
        }))
    }
}
package com.priyoaujla

import com.google.gson.JsonObject

data class BusinessEvent(val name: Name, val payload: JsonObject) {
    enum class Name(val value: String) {
        Subscribed("subscribed"),
        CampaignCreated("campaign-created"),
        CampaignEmailSent("campaign-email-sent"),
        UnSubscribed("un-subscribed")
    }
}
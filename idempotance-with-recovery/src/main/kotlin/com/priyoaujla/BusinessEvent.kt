package com.priyoaujla

import com.fasterxml.jackson.databind.JsonNode

data class BusinessEvent(val name: Name, val payload: JsonNode) {
    enum class Name(value: String) {
        Subscribed("subscribed"),
        CampaignCreated("campaign-created"),
        CampaignEmailSent("campaign-email-sent"),
        UnSubscribed("un-subscribed")
    }
}
package com.priyoaujla

import org.junit.Test
import java.util.*

class StressTest {


    @Test fun `test emails are only sent once and not sent when unsubscribed`() {
        val storage = BusinessEventStorage(dataSource)
        val emailAddress = EmailAddress("${UUID.randomUUID()}@example.com")
        val subscriber = MailingListSubscriber(ThreadSafeEmailInbox(emailAddress), storage)
        subscriber.subscribe()


    }
}
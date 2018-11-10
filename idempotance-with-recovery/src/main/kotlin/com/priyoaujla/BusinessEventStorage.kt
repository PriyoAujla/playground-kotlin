package com.priyoaujla

import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

class BusinessEventStorage(
        private val dataSource: DataSource
) {

    fun insert(event: BusinessEvent) {
        val sql = """
            INSERT INTO events(type, created, payload) VALUES(?,?,?::JSON);
        """.trimIndent()
        dataSource.update(sql) {
            it.setString(1, event.name.value)
            it.setTimestamp(2, Timestamp.from(Instant.now()))
            it.setObject(3, event.payload.toString())
            it.executeUpdate()
        }
    }
}

fun DataSource.update(sql: String, block: (PreparedStatement) -> Unit) {
    connection.prepareStatement(sql).use(block)
}
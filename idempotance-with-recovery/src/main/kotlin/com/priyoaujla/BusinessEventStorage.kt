package com.priyoaujla

import com.google.gson.JsonParser
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource


class BusinessEventStorage(
        private val dataSource: DataSource
) {

    private var parser = JsonParser()

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

    fun all(): Sequence<BusinessEvent> {
        val pageSize = 100
        return generateSequence({takeFrom(0, pageSize)}) {
            if(it.size < pageSize) {
                null
            } else {
                takeFrom(it.last().id, pageSize)
            }
        }.flatMap { it.map { it.event }.asSequence() }
    }

    private fun takeFrom(startId: Long, size: Int): List<StoredBusinessEvent> {
        val sql = """
                SELECT * FROM events WHERE id=? LIMIT ?;
            """.trimIndent()
        return dataSource.fetch(sql) {
            it.setLong(1, startId)
            it.setInt(2, size)
            val resultSet = it.executeQuery()

            val result = mutableListOf<StoredBusinessEvent>()

            while(resultSet.next()) {
                result.add(resultSet.toEvent())
            }

            result
        }
    }

    private class StoredBusinessEvent(val id: Long, val event: BusinessEvent)

    private fun DataSource.update(sql: String, block: (PreparedStatement) -> Unit) {
        connection.prepareStatement(sql).use(block)
    }

    private fun DataSource.fetch(sql: String, block: (PreparedStatement) -> List<StoredBusinessEvent>): List<StoredBusinessEvent> {
        return connection.prepareStatement(sql).use(block)
    }

    private fun ResultSet.toEvent(): StoredBusinessEvent =
        StoredBusinessEvent(this.getLong(1), BusinessEvent(BusinessEvent.Name.valueOf(this.getString(2)), parser.parse(this.getString(4))))
}


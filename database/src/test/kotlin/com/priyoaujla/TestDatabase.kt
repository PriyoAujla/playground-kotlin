package com.priyoaujla

import com.zaxxer.hikari.HikariDataSource
import org.hsqldb.Server
import org.junit.rules.ExternalResource
import java.sql.Connection
import javax.sql.DataSource

class TestDatabase(
    val before: (Connection) -> Unit = {},
    val after: (Connection) -> Unit = {},
    val databasePort: Int
) : ExternalResource() {

    private val server = Server()

    val dataSource: DataSource by lazy {
        HikariDataSource().apply {
            jdbcUrl = "jdbc:hsqldb:hsql://localhost:$databasePort/mymemdb"
            username = "SA"
            password = ""
        }
    }

    override fun before() {
        server.setDatabaseName(0, "mymemdb")
        server.setDatabasePath(0, "mem:mymemdb")
        server.setPort(databasePort)
        server.setDaemon(true)
        server.start()

        before(dataSource.connection)
    }

    override fun after() {
        after(dataSource.connection)
        server.stop()
    }
}


package com.priyoaujla

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource


val dataSource: DataSource by lazy {
    HikariDataSource().apply {
        val pgSimpleDataSource = PGSimpleDataSource().apply {
            serverName = "localhost"
            databaseName = "playground"
            user = "playground_user"
            password = "playground"
        }

        pgSimpleDataSource.migrate()
        dataSource = pgSimpleDataSource
    }
}

private fun DataSource.migrate() {
    val configuration = Flyway.configure().dataSource(this)
    configuration.locations(Location("database-migrations"))
    val flyway = configuration.load()
    flyway.migrate()
}
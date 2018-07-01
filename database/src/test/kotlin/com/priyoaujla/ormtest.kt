package com.priyoaujla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.sql.Connection

class OrmTest {

    @JvmField
    @Rule
    val database = TestDatabase(before = ::setup, after = ::tearDown, databasePort = 9002)

    private val userTable: Table<User> = UserTable("user", database.dataSource)

    private fun setup(connection: Connection) {
        connection.use {
            it.createStatement().executeUpdate("""CREATE TABLE user (
            | id INT NOT NULL IDENTITY,
            | name VARCHAR(255) NOT NULL,
            | age INT NOT NULL,
            | fav_colour VARCHAR(64),
            | PRIMARY KEY (id)
            | );
        """.trimMargin())
        }
    }

    private fun tearDown(connection: Connection) {
        connection.use {
            it.createStatement().executeUpdate("""DROP TABLE user;""".trimMargin())
        }
    }

    @Test
    fun `inserting and retrieving`() {
        val user = User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange"))
        userTable.insert(user)
        val result = userTable.get(UserTable.idColumn to UserTable.idColumn.withValue(0))

        assertEquals(user, result)
    }

    @Test
    fun `inserting and updating`() {
        userTable.insert(User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange")))
        val oldUser = userTable.get(UserTable.idColumn to UserTable.idColumn.withValue(0))!!
        val newUser = oldUser.copy(name = Name("Julie"), age = Age(55), favColour = Colour("Blue"))
        userTable.update(newUser)
        val result = userTable.get(UserTable.idColumn to UserTable.idColumn.withValue(0))

        assertEquals(newUser, result)
    }

    @Test
    fun `inserting and deleting`() {
        userTable.insert(User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange")))
        userTable.delete(userTable.get(UserTable.idColumn to UserTable.idColumn.withValue(0))!!)

        assertNull(userTable.get(UserTable.idColumn to UserTable.idColumn.withValue(0)))
    }
}


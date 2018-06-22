package com.priyoaujla

import com.zaxxer.hikari.HikariDataSource
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource


data class ColumnName(val value: String)
data class ColumnValue(val column: Column, val valueAsSqlString: ValueAsSqlString, val value: Any, val setter: (Int, PreparedStatement) -> PreparedStatement)

data class ValueAsSqlString(val value: String)

val dataSource: DataSource by lazy {
    val dataSource = HikariDataSource().apply {
        jdbcUrl = "jdbc:hsqldb:mem:mymemdb"
        username = "SA"
        password = ""
    }

    dataSource.connection.use {
        it.createStatement().executeUpdate("""CREATE TABLE user (
            | id INT NOT NULL IDENTITY,
            | name VARCHAR(255) NOT NULL,
            | age INT NOT NULL,
            | fav_colour VARCHAR(64),
            | PRIMARY KEY (id)
            | );
        """.trimMargin())
    }

    dataSource
}

interface Column {
    val name: ColumnName
    val required: Boolean

    fun toColumnValue(valueAsSqlString: ValueAsSqlString, value: Any, setter: (Int, PreparedStatement) -> PreparedStatement): ColumnValue = ColumnValue(this, valueAsSqlString, value, setter)
}

interface TypeColumn<in T>: Column {

    fun withValue(value: T): ColumnValue
    fun toSqlString(value: T): ValueAsSqlString
}


data class StringColumn(override val name: ColumnName, override val required: Boolean) : TypeColumn<String> {

    override fun withValue(value: String) = toColumnValue(toSqlString(value), value) { index, it ->
        it.apply {
            setString(index, value)
        }
    }

    override fun toSqlString(value: String): ValueAsSqlString = ValueAsSqlString("'$value'")
}

data class IntColumn(override val name: ColumnName, override val required: Boolean) : TypeColumn<Int> {

    override fun withValue(value: Int) = toColumnValue(toSqlString(value), value) { index, it ->
        it.apply {
            setInt(index, value)
        }
    }
    override fun toSqlString(value: Int): ValueAsSqlString = ValueAsSqlString("$value")
}

interface IdColumn<in T> : TypeColumn<T>

data class KeyColumn<in T>(val column: TypeColumn<T>) : IdColumn<T> {

    override val name: ColumnName get() = column.name

    override val required: Boolean get() = column.required

    override fun withValue(value: T): ColumnValue {
        val delegate = column.withValue(value)
        return delegate.copy(column = this)
    }

    override fun toSqlString(value: T): ValueAsSqlString = column.toSqlString(value)
}

data class AutoGeneratedKeyColumn<in T>(val column: KeyColumn<T>) : IdColumn<T> by column {
    override fun withValue(value: T): ColumnValue {
        val delegate = column.withValue(value)
        return delegate.copy(column = this)
    }
}

abstract class Table<T>(private val dataSource: DataSource) {

    internal abstract val name: String
    internal abstract val columns: Set<Column>

    fun insert(thing: T) {
        val values = mapTo(thing)
        val nonAutoGeneratedColumns =
                columns.filter { it !is AutoGeneratedKeyColumn<*> }
                        .filter { column -> column.required || values.any { column == it.column } }
        if (values.all { nonAutoGeneratedColumns.contains(it.column) }) {
//            val sqlInsert = """
//                INSERT INTO ${name}(${nonAutoGeneratedColumns.map { it.name.value }.joinToString(", ")})
//                    VALUES(${values.map { it.valueAsSqlString.value }.joinToString(", ")});
//            """.trimIndent()
//
//            println(sqlInsert)

            this.dataSource.connection.use {
                val sqlInsert = """
                INSERT INTO ${name}(${nonAutoGeneratedColumns.map { it.name.value }.joinToString(", ")})
                    VALUES(${values.map { "?" }.joinToString(", ")});
            """.trimIndent()

                val preparedStatement = it.prepareStatement(sqlInsert)
                values.forEachIndexed { index, it ->
                    it.setter(index + 1, preparedStatement)
                }

                preparedStatement.execute()
            }
        } else {
            error("Oops something went wrong")
        }
    }

    fun update(thing: T) {
        val newValues = mapTo(thing) + uniqueKey(thing)
        val id = newValues.find { it.column is IdColumn<*>}
        if (id != null && columns.contains(id.column)) {
//            val sqlUpdate = """
//                UPDATE
//                    SET ${newValues.filter { it.column !is AutoGeneratedKeyColumn<*> }.map { "${it.column.name.value} = ${it.valueAsSqlString.value}" }.joinToString(", ")}
//                FROM ${name}
//                    WHERE ${id.column.name.value} = ${id.valueAsSqlString.value}
//            """.trimIndent()
//            println(sqlUpdate)

            this.dataSource.connection.use {
                val sqlUpdate = """
                UPDATE ${name}
                    SET ${newValues.filter { it.column !is AutoGeneratedKeyColumn<*> }.map { "${it.column.name.value} = ?" }.joinToString(", ")}
                    WHERE ${id.column.name.value} = ?
            """.trimIndent()

                val preparedStatement = it.prepareStatement(sqlUpdate)
                (newValues + id).forEachIndexed { index, it ->
                    it.setter(index + 1, preparedStatement)
                }

                preparedStatement.execute()
            }
        } else {
            error("uh oh! Couldn't update!")
        }
    }

    fun delete(thing: T) {
        val columnValue = uniqueKey(thing)
        val isAnIdentifier = columnValue.column is IdColumn<*>
        val tableHasSuchAColumn = columns.contains(columnValue.column)
        if (isAnIdentifier && tableHasSuchAColumn) {
                val sqlDelete = """
                    DELETE FROM ${name} WHERE ${columnValue.column.name.value} = ${columnValue.valueAsSqlString.value}
                """.trimIndent()

                println(sqlDelete)
            } else {
                error("Column value is not a key or is not a valid column")
            }
    }

    fun get(id: Pair<IdColumn<*>, ColumnValue>): T? {
        this.dataSource.connection.use {
            val sqlUpdate = """
                SELECT *
                    FROM ${name}
                    WHERE ${id.first.name.value} = ?
            """.trimIndent()

            val preparedStatement = it.prepareStatement(sqlUpdate)
            listOf(id.second).forEachIndexed { index, columnValue ->
                columnValue.setter(index + 1, preparedStatement)
            }

            return preparedStatement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    mapTo(resultSet)
                } else {
                    null
                }
            }
        }
    }

    internal abstract fun mapTo(thing: T): Set<ColumnValue>
    internal abstract fun uniqueKey(thing: T): ColumnValue
    internal abstract fun mapTo(resultSet: ResultSet): T?
}

class UserTable(override val name: String, dataSource: DataSource) : Table<User>(dataSource) {

    companion object {
        val idColumn = AutoGeneratedKeyColumn(KeyColumn(IntColumn(ColumnName("id"), required = true)))
        val nameColumn = NameColumn(required = true)
        val ageColumn = AgeColumn(required = true)
        val favColourColumn = ColourColumn(required = false)
    }

    override val columns = setOf(
            idColumn,
            nameColumn,
            ageColumn,
            favColourColumn
    )

    override fun mapTo(thing: User): Set<ColumnValue> {
        val columnValues = mutableSetOf(
                nameColumn.withValue(thing.name),
                ageColumn.withValue(thing.age)
        )

        thing.favColour?.let { columnValues.add(favColourColumn.withValue(it)) }
        return columnValues
    }

    override fun mapTo(resultSet: ResultSet): User {
        return User(
            resultSet.getInt(idColumn.name.value),
            Name(resultSet.getString(nameColumn.name.value)),
            Age(resultSet.getInt(ageColumn.name.value)),
            Colour(resultSet.getString(favColourColumn.name.value))
        )
    }

    override fun uniqueKey(thing: User) = idColumn.withValue(thing.id)
}

data class Name(val value: String)
data class NameColumn(
        override val name: ColumnName = ColumnName("name"),
        override val required: Boolean
): TypeColumn<Name> {

    private val delegate = StringColumn(name, required)

    override fun withValue(value: Name): ColumnValue = delegate.withValue(value.value).copy(column = this)

    override fun toSqlString(value: Name): ValueAsSqlString = delegate.toSqlString(value.value)
}

data class Age(val value: Int)
data class AgeColumn(
        override val name: ColumnName = ColumnName("age"),
        override val required: Boolean
): TypeColumn<Age> {

    private val delegate = IntColumn(name, required)

    override fun withValue(value: Age): ColumnValue = delegate.withValue(value.value).copy(column = this)

    override fun toSqlString(value: Age): ValueAsSqlString = delegate.toSqlString(value.value)
}

data class Colour(val value: String)
data class ColourColumn(
        override val name: ColumnName = ColumnName("fav_colour"),
        override val required: Boolean
): TypeColumn<Colour> {

    private val delegate = StringColumn(name, required)

    override fun withValue(value: Colour): ColumnValue = delegate.withValue(value.value).copy(column = this)

    override fun toSqlString(value: Colour): ValueAsSqlString = delegate.toSqlString(value.value)
}

data class User(val id: Int = 0, val name: Name, val age: Age, val favColour: Colour?)

fun main(args: Array<String>) {

    val userTable: Table<User> = UserTable("user", dataSource)
    userTable.insert(User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange")))
    userTable.update(User(1, Name("Robert"), Age(34), Colour("Blue")))

    println(userTable.get(UserTable.idColumn to UserTable.idColumn.withValue(1)))
//    userTable.delete(User(1, Name("some name"), Age(34), Colour("fav colour")))

}
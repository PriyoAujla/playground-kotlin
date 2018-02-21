package com.priyoaujla

data class ColumnName(val value: String)
data class ColumnValue(val column: Column, val valueAsSqlString: ValueAsSqlString)
data class ValueAsSqlString(val value: String)

interface Column {
    val name: ColumnName
    val required: Boolean

    fun toColumnValue(valueAsSqlString: ValueAsSqlString): ColumnValue = ColumnValue(this, valueAsSqlString)

}

interface TypeColumn<in T>: Column {

    fun withValue(value: T): ColumnValue
    fun toSqlString(value: T): ValueAsSqlString
}


data class StringColumn(override val name: ColumnName, override val required: Boolean) : TypeColumn<String> {

    override fun withValue(value: String) = toColumnValue(toSqlString(value))

    override fun toSqlString(value: String): ValueAsSqlString = ValueAsSqlString("'$value'")
}

data class IntColumn(override val name: ColumnName, override val required: Boolean) : TypeColumn<Int> {

    override fun withValue(value: Int) = toColumnValue(toSqlString(value))
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

abstract class Table<in T> {

    internal abstract val name: String
    internal abstract val columns: Set<Column>

    fun insert(thing: T) {
        val values = mapTo(thing)
        val nonAutoGeneratedColumns =
                columns.filter { it !is AutoGeneratedKeyColumn<*> }
                        .filter { column -> column.required || values.any { column == it.column } }
        if (values.all { nonAutoGeneratedColumns.contains(it.column) }) {
            val sqlInsert = """
                INSERT INTO ${name}(${nonAutoGeneratedColumns.map { it.name.value }.joinToString(", ")})
                    VALUES(${values.map { it.valueAsSqlString.value }.joinToString(", ")});
            """.trimIndent()

            println(sqlInsert)
        } else {
            error("Oops something went wrong")
        }
    }

    fun update(thing: T) {
        val newValues = mapTo(thing) + uniqueKey(thing)
        val id = newValues.find { it.column is IdColumn<*>}
        if (id != null && columns.contains(id.column)) {
            val sqlUpdate = """
                UPDATE
                    SET ${newValues.filter { it.column !is AutoGeneratedKeyColumn<*> }.map { "${it.column.name.value} = ${it.valueAsSqlString.value}" }.joinToString(", ")}
                FROM ${name}
                    WHERE ${id.column.name.value} = ${id.valueAsSqlString.value}
            """.trimIndent()
            println(sqlUpdate)
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

    abstract fun mapTo(thing: T): Set<ColumnValue>

    abstract fun uniqueKey(thing: T): ColumnValue
}

class UserTable(override val name: String) : Table<User>() {

    private val idColumn = AutoGeneratedKeyColumn(KeyColumn(IntColumn(ColumnName("id"), true)))
    private val nameColumn = NameColumn(required = true)
    private val ageColumn = AgeColumn(required = true)
    private val favColourColumn = ColourColumn(required = false)

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

    val userTable = UserTable("user")
    userTable.insert(User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange")))
    userTable.delete(User(1, Name("some name"), Age(34), Colour("fav colour")))
    userTable.update(User(1, Name("Robert"), Age(34), Colour("Blue")))

}
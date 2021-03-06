package com.priyoaujla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

class OrmTest {

    @JvmField
    @Rule
    val database = TestDatabase(before = ::setup, after = ::tearDown, databasePort = 9002)

    private val userTable: Table<User> = UserTable("user", database.dataSource)
    private val billTable: Table<Bill> = BillTable("bill", database.dataSource)
    private val documentTable: Table<Document> = DocumentTable("document", database.dataSource)

    private fun setup(connection: Connection) {
        connection.use {

            // simple example
            it.createStatement().executeUpdate("""CREATE TABLE user (
            | id INT NOT NULL IDENTITY,
            | name VARCHAR(255) NOT NULL,
            | age INT DEFAULT NULL,
            | fav_colour VARCHAR(64) DEFAULT NULL,
            | PRIMARY KEY (id)
            | );
        """.trimMargin())

            // non numerical primary key example
            it.createStatement().executeUpdate("""CREATE TABLE bill (
            | id UUID NOT NULL,
            | amount DECIMAL NOT NULL,
            | PRIMARY KEY (id)
            | );
        """.trimMargin())

            // composite key example
            it.createStatement().executeUpdate("""CREATE TABLE document (
            | id VARCHAR(255) NOT NULL,
            | version INT NOT NULL,
            | text CLOB NOT NULL,
            | PRIMARY KEY (id, version)
            | );
        """.trimMargin())
        }
    }

    private fun tearDown(connection: Connection) {
        connection.use {
            it.createStatement().executeUpdate("""DROP TABLE user;""".trimMargin())
            it.createStatement().executeUpdate("""DROP TABLE bill;""".trimMargin())
            it.createStatement().executeUpdate("""DROP TABLE document;""".trimMargin())
        }
    }

    @Test
    fun `inserting and retrieving`() {
        val user = User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange"))
        userTable.insert(user)
        val result = userTable.get(UserTable.idColumn.withValue(0))

        assertEquals(user, result)
    }

    @Test
    fun `inserting and updating`() {
        userTable.insert(User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange")))
        val oldUser = userTable.get(UserTable.idColumn.withValue(0))!!
        val newUser = oldUser.copy(name = Name("Julie"), age = Age(55), favColour = Colour("Blue"))
        userTable.update(newUser)
        val result = userTable.get(UserTable.idColumn.withValue(0))

        assertEquals(newUser, result)
    }

    @Test
    fun `inserting and deleting`() {
        userTable.insert(User(name = Name("Betty"), age = Age(23), favColour = Colour("Orange")))
        userTable.delete(userTable.get(UserTable.idColumn.withValue(0))!!)

        assertNull(userTable.get(UserTable.idColumn.withValue(0)))
    }

    @Test
    fun `inserting nullable value`() {
        val user = User(name = Name("Betty"), age = null, favColour = null)
        userTable.insert(user)
        val result = userTable.get(UserTable.idColumn.withValue(0))

        assertEquals(user, result)
    }

    @Test
    fun `inserting and retrieving uuid based primary key`() {
        val uuid = UUID.randomUUID()
        val bill = Bill(id = uuid, amount = Money(BigDecimal(23)))
        billTable.insert(bill)
        val result = billTable.get(BillTable.idColumn.withValue(uuid))!!

        assertEquals(bill, result)
    }

    @Test
    fun `inserting, updating and deleting when composite key`() {
        val uuid = UUID.randomUUID()
        val document1 = Document(id = uuid, version = 1, text = "Hello world")
        val document2 = Document(id = uuid, version = 2, text = "World Hello")

        documentTable.insert(document1)
        documentTable.insert(document2)
        assertEquals(document1, documentTable.get(DocumentTable.idColumn.withValue(uuid), DocumentTable.versionColumn.withValue(1))!!)

        val updatedDocument = document1.copy(text = "Hello world!")
        documentTable.update(updatedDocument)
        assertEquals(updatedDocument, documentTable.get(DocumentTable.idColumn.withValue(uuid), DocumentTable.versionColumn.withValue(1))!!)
        assertEquals(document2, documentTable.get(DocumentTable.idColumn.withValue(uuid), DocumentTable.versionColumn.withValue(2))!!)

        documentTable.delete(updatedDocument)
        assertNull(documentTable.get(DocumentTable.idColumn.withValue(uuid), DocumentTable.versionColumn.withValue(1)))
        assertEquals(document2, documentTable.get(DocumentTable.idColumn.withValue(uuid), DocumentTable.versionColumn.withValue(2))!!)
    }

    @Test
    fun `find by a column`() {
        val bill1 = Bill(amount = Money(BigDecimal(20)))
        val bill2 = Bill(amount = Money(BigDecimal(20)))
        val bill3 = Bill(amount = Money(BigDecimal(20)))
        val bill4 = Bill(amount = Money(BigDecimal(21)))
        billTable.insert(bill1)
        billTable.insert(bill2)
        billTable.insert(bill3)
        billTable.insert(bill4)

        assertEquals(billTable.findBy(BillTable.amountColumn.withValue(Money(BigDecimal(20)))).toSet(), setOf(bill1, bill2, bill3))
    }

    @Test
    fun `return all`() {
        val bill1 = Bill(amount = Money(BigDecimal(20)))
        val bill2 = Bill(amount = Money(BigDecimal(20)))
        val bill3 = Bill(amount = Money(BigDecimal(20)))
        val bill4 = Bill(amount = Money(BigDecimal(21)))
        billTable.insert(bill1)
        billTable.insert(bill2)
        billTable.insert(bill3)
        billTable.insert(bill4)

        assertEquals(billTable.all().toSet(), setOf(bill1, bill2, bill3, bill4))
    }
}


class UserTable(override val name: String, dataSource: DataSource) : Table<User>(dataSource, iterationPageSize = 2) {

    companion object {
        val idColumn = AutoGeneratedKeyColumn(KeyColumn(IntColumn(ColumnName("id"))))
        val nameColumn = NameColumn()
        val ageColumn = AgeColumn()
        val favColourColumn = ColourColumn()
    }

    override fun mapTo(thing: User): Set<ColumnValueSetter> {
        return mutableSetOf(
                idColumn.withValue(thing.id),
                nameColumn.withValue(thing.name),
                ageColumn.withValue(thing.age),
                favColourColumn.withValue(thing.favColour)
        )
    }

    override fun mapFrom(resultSet: ResultSet): User {
        return User(
                resultSet.getInt(idColumn.name.value),
                Name(resultSet.getString(nameColumn.name.value)),
                resultSet.getNullableInt(ageColumn.name.value)?.let { Age(it) },
                resultSet.getString(favColourColumn.name.value)?.let { Colour(it) }
        )
    }
}

data class Name(val value: String)
data class NameColumn(
        override val name: ColumnName = ColumnName("name")
) : TypeColumn<Name> {

    private val delegate = StringColumn(name)

    override fun withValue(value: Name?): ColumnValueSetter = delegate.withValue(value?.value).copy(column = this)
}

data class Age(val value: Int)
data class AgeColumn(
        override val name: ColumnName = ColumnName("age")
) : TypeColumn<Age> {

    private val delegate = IntColumn(name)

    override fun withValue(value: Age?): ColumnValueSetter = delegate.withValue(value?.value).copy(column = this)
}

data class Colour(val value: String)
data class ColourColumn(
        override val name: ColumnName = ColumnName("fav_colour")
) : TypeColumn<Colour> {

    private val delegate = StringColumn(name)

    override fun withValue(value: Colour?): ColumnValueSetter = delegate.withValue(value?.value).copy(column = this)
}

data class User(val id: Int = 0, val name: Name, val age: Age?, val favColour: Colour?)

data class Money(val value: BigDecimal)
data class AmountColumn(
        override val name: ColumnName = ColumnName("amount")
) : TypeColumn<Money> {

    private val delegate = BigDecimalColumn(name)

    override fun withValue(value: Money?): ColumnValueSetter = delegate.withValue(value?.value).copy(column = this)
}
data class UUIDColumn(
        override val name: ColumnName = ColumnName("id")
): TypeColumn<UUID> {

    private val delegate = StringColumn(name)

    override fun withValue(value: UUID?): ColumnValueSetter = delegate.withValue(value.toString()).copy(column = this)
}
data class Bill(val id: UUID = UUID.randomUUID(), val amount: Money)

class BillTable(override val name: String, dataSource: DataSource) : Table<Bill>(dataSource, iterationPageSize = 2) {

    companion object {
        val idColumn = KeyColumn(UUIDColumn(ColumnName("id")))
        val amountColumn = AmountColumn()
    }

    override fun mapTo(thing: Bill): Set<ColumnValueSetter> {
        return mutableSetOf(
                idColumn.withValue(thing.id),
                amountColumn.withValue(thing.amount)
        )
    }

    override fun mapFrom(resultSet: ResultSet): Bill {
        return Bill(
                UUID.fromString(resultSet.getString(idColumn.name.value)),
                Money(resultSet.getBigDecimal(amountColumn.name.value))
        )
    }
}

data class Document(val id: UUID, val version: Int, val text: String)
class DocumentTable(override val name: String, dataSource: DataSource) : Table<Document>(dataSource, iterationPageSize = 2) {

    companion object {
        val idColumn = KeyColumn(UUIDColumn(ColumnName("id")))
        val versionColumn = KeyColumn(IntColumn(ColumnName("version")))
        val textColumn = StringColumn(ColumnName("text"))
    }

    override fun mapTo(thing: Document): Set<ColumnValueSetter> {
        return mutableSetOf(
                idColumn.withValue(thing.id),
                versionColumn.withValue(thing.version),
                textColumn.withValue(thing.text)
        )
    }

    override fun mapFrom(resultSet: ResultSet): Document {
        return Document(
                UUID.fromString(resultSet.getString(idColumn.name.value)),
                resultSet.getInt(versionColumn.name.value),
                resultSet.getString(textColumn.name.value)
        )
    }
}
import groovy.sql.Sql
import java.io.Closeable
import java.io.File

class StaticDatabase private constructor(
    private val sql: Sql,
    private val tableNames: Array<String>
) : Closeable {

    fun insertAll(queryList: List<SqlQuery>) {
        queryList.forEach { insert(it) }
    }

    private fun insert(queryProvider: SqlQuery) {
        queryProvider.parameters.forEach {
            sql.executeInsert(queryProvider.query, it)
        }
    }

    fun updateAll(queryList: List<String>) {
        queryList.forEach {
            update(it)
        }
    }

    private fun update(query: String) {
        Log.d("Executing Update query : $query")
        sql.executeUpdate(query)
    }

    private fun logTableCount(name: String) {
        val count = sql.firstRow("SELECT count(*) FROM $name", arrayListOf()).toString()
        Log.i("$name: $count")
    }

    override fun close() {
        tableNames.forEach {
            logTableCount(it)
        }
        sql.close()
    }

    companion object {

        private const val REAL_TYPE = "REAL"
        private const val TEXT_TYPE = "TEXT"
        private const val INTEGER_TYPE = "INTEGER"

        private fun openSQL(dbFile: File, tableNameAndFieldsMap: Map<String, List<Field>>) =
            Sql.newInstance("$SQL_DB_URL_PREFIX:$dbFile", DRIVER_CLASSNAME).apply {

                for ((tableName, fields) in tableNameAndFieldsMap) {
                    val name = tableName.tableNameAdjustment()
                    Log.d("Creating table $name")
                    val sql = createSql(tableName, fields)
                    Log.d("sql: $sql")
                    execute(sql)
                }
            }

        private fun createSql(tableName: String, fields: List<Field>): String {
            var str = "CREATE TABLE IF NOT EXISTS `$tableName`"
            str += " ("
            fields.forEach { field ->
                str += "`${field.name.fieldAdjustment()}` ${getFieldType(field.fieldType)}"
                if (field.name == "__KEY")
                    str += " NOT NULL"
                str += ", "
            }
            str += "`__KEY` TEXT NOT NULL, `__STAMP` INTEGER, `__GlobalStamp` INTEGER, `__TIMESTAMP` TEXT, PRIMARY KEY(`__KEY`))"
            return str
        }

        private fun getFieldType(fieldType: Int?): String = when (fieldType) {
            0 -> TEXT_TYPE
            1 -> REAL_TYPE
            2 -> TEXT_TYPE
            3 -> TEXT_TYPE
            4 -> TEXT_TYPE
            5 -> TEXT_TYPE
            6 -> INTEGER_TYPE
            7 -> TEXT_TYPE
            8 -> INTEGER_TYPE
            9 -> INTEGER_TYPE
            11 -> TEXT_TYPE
            12 -> TEXT_TYPE
            25 -> INTEGER_TYPE
            38 -> TEXT_TYPE
            else -> TEXT_TYPE
        }

        fun initialize(dbFile: File, tableNameAndFieldsMap: Map<String, List<Field>>) =
            StaticDatabase(openSQL(dbFile, tableNameAndFieldsMap), tableNameAndFieldsMap.keys.toTypedArray())
    }

    fun <R> useInTransaction(block: (StaticDatabase) -> R) = use {
        sql.withTransaction(closureOf<Any> {
            block(this@StaticDatabase)
        })
    }
}
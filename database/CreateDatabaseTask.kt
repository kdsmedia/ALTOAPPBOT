import org.json.JSONObject
import java.io.File

class CreateDatabaseTask(
    private val dataModelList: List<DataModel>,
    private val assetsPath: String,
    dbFilePath: String
) {

    private val originalTableNamesMap: Map<String, String> = getTableNamesMap()
    private val tableNameAndFieldsMap: Map<String, List<Field>> = getTableNameAndFieldsMap()

    // PARAMS
    private val dbFile = File(dbFilePath)

    private val propertyListMap = mutableMapOf<String, List<String>>()
    private val relatedEntitiesMapList =
        mutableListOf<MutableMap<String, MutableList<JSONObject>>>()
    private val updateQueryHolderList = mutableListOf<UpdateQueryHolder>()

    private var initialGlobalStamp = 0
    private val dumpedTables = mutableListOf<String>()

    private fun getTableNamesMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        dataModelList.forEach { dataModel ->
            map[dataModel.name.tableNameAdjustment()] = dataModel.name
        }
        return map
    }

    private fun getTableNameAndFieldsMap(): Map<String, List<Field>> {
        val map = mutableMapOf<String, List<Field>>()
        dataModelList.forEach { dataModel ->
            map[dataModel.name.tableNameAdjustment()] = dataModel.fields?.filter { it.path.isNullOrEmpty() } ?: mutableListOf()
        }
        return map
    }

    init {
        taskAction()
    }

    private fun taskAction() {
        dbFile.delete()
        StaticDatabase.initialize(dbFile, tableNameAndFieldsMap)
            .useInTransaction { database ->

                val staticDataInitializer = StaticDataInitializer()

                database.insertAll(getSqlQueries(staticDataInitializer))
                database.updateAll(getUpdateQueries())
                Log.d("Database updated")
                val dumpInfoFile = getDumpedInfoFile(assetsPath)
                integrateGlobalStamp(dumpInfoFile, initialGlobalStamp)
                integrateDumpedTables(dumpInfoFile, dumpedTables)
            }
    }

    private fun getSqlQueries(staticDataInitializer: StaticDataInitializer): List<SqlQuery> {
        val queryList = mutableListOf<SqlQuery>()

        Log.d("originalTableNamesMap = $originalTableNamesMap")
        Log.d("tableNameAndFieldsMap = $tableNameAndFieldsMap")

        for ((tableName, tableNameOriginal) in originalTableNamesMap) {
            tableNameAndFieldsMap[tableName]?.let { fields ->
                Log.d("tableName: $tableName, tableNameOriginal: $tableNameOriginal")

                Log.d("[$tableName] fields : ${fields.joinToString { it.name }}")
                queryList.addAll(
                    getSqlQueriesForTable(
                        tableName,
                        tableNameOriginal,
                        fields,
                        staticDataInitializer
                    )
                )
            }
        }

        relatedEntitiesMapList.forEach { relatedEntitiesMap ->
            queryList.addAll(getSqlQueriesForRelatedTables(staticDataInitializer, relatedEntitiesMap))
        }

        return filterUnique(queryList)
    }

    private fun filterUnique(queries: MutableList<SqlQuery>): MutableList<SqlQuery> {

        val newQueries = mutableListOf<SqlQuery>()

        queries.forEach { sqlQuery ->

            var keyPropertyIndex = propertyListMap[sqlQuery.tableName]?.indexOf("__KEY") ?: 0
            if (keyPropertyIndex == -1) keyPropertyIndex = 0

            sqlQuery.parameters.forEach { array ->
                val key = array[keyPropertyIndex]
                key?.let {
                    val siblingQueries = queries
                        .filter { it.query == sqlQuery.query } // all queries for the same table

                    val newArray = arrayOfNulls<Any>(array.size)
                    array.indices.forEach { index ->
                        newArray[index] = array[index]
                        siblingQueries.forEach { siblingQuery ->

                            siblingQuery.parameters.forEach { arr -> // each line of parameters (one entity)
                                if (arr[keyPropertyIndex] == key && newArray[index] == null) {
                                    newArray[index] = arr[index]
                                }
                            }
                        }
                    }

                    val newQuery = SqlQuery(sqlQuery.query, listOf(newArray), sqlQuery.tableName)
                    if (newQueries
                            .filter { it.query == newQuery.query }
                            .firstOrNull {
                                it.parameters.firstOrNull()?.get(keyPropertyIndex) == key
                            } == null
                    ) {
                        newQueries.add(newQuery)
                    }
                }
            }
        }
        return newQueries
    }

    private fun getSqlQueriesForRelatedTables(staticDataInitializer: StaticDataInitializer, relatedEntitiesMap: MutableMap<String, MutableList<JSONObject>>): List<SqlQuery> {
        val queryList = mutableListOf<SqlQuery>()
        val innerRelatedEntitiesMapList = mutableListOf<MutableMap<String, MutableList<JSONObject>>>()
        for ((originalTableName, jsonEntityList) in relatedEntitiesMap) {
            Log.d("originalTableName $originalTableName")
            Log.d("originalTableNamesMap $originalTableNamesMap")
            val tableName = originalTableNamesMap.filter { it.value == originalTableName }.keys.firstOrNull()
            Log.d("tableName $tableName")
            val relatedTableFields = tableNameAndFieldsMap[tableName]
            Log.d("relatedTableFields: ${relatedTableFields?.joinToString { it.name }}")
            if (tableName != null && relatedTableFields != null) {

                jsonEntityList.forEach { jsonEntity ->
                    Log.d("getSqlQueriesForRelatedTables\n")
                    Log.d("jsonEntity = $jsonEntity")

                    val sqlQueryBuilder = SqlQueryBuilder(jsonEntity, relatedTableFields)

                    innerRelatedEntitiesMapList.add(sqlQueryBuilder.relatedEntitiesMap)
                    updateQueryHolderList.addAll(sqlQueryBuilder.updateQueryHolderList)

                    queryList.add(
                        getQueryFromSqlQueryBuilder(
                            sqlQueryBuilder,
                            tableName,
                            staticDataInitializer
                        )
                    )
                }
            }
        }
        innerRelatedEntitiesMapList.forEach { innerRelatedEntitiesMap ->
            queryList.addAll(getSqlQueriesForRelatedTables(staticDataInitializer, innerRelatedEntitiesMap))
        }
        return queryList
    }

    private fun getUpdateQueries(): List<String> {
        val queryList = mutableListOf<String>()
        updateQueryHolderList.forEach { updateQueryHolder ->
            dataModelList.find { it.name.tableNameAdjustment() == updateQueryHolder.relatedDataClass }?.let { dataModel ->
                dataModel.relations?.find { it.inverseName.fieldAdjustment() == updateQueryHolder.oneToManyRelationName }?.let { manyToOneRelation ->
                    val query = "UPDATE ${dataModel.name.tableNameAdjustment()} SET __${manyToOneRelation.name.fieldAdjustment()}Key = ${updateQueryHolder.relationKey} WHERE __KEY = ${updateQueryHolder.entityKey}"
                    queryList.add(query)
                }
            }
        }
        return queryList.distinct()
    }

    private fun getSqlQueriesForTable(
        tableName: String,
        tableNameOriginal: String,
        fields: List<Field>,
        staticDataInitializer: StaticDataInitializer
    ): List<SqlQuery> {

        val sqlQueryList = mutableListOf<SqlQuery>()

        val filePath = getDataPath(assetsPath, tableNameOriginal)
        val entitySqlQueriesFile = File(filePath)

        Log.d("[$tableName] Reading data at path $filePath")

        if (entitySqlQueriesFile.exists()) {
            getSqlQueriesForTableFromFile(
                tableName,
                entitySqlQueriesFile,
                fields,
                staticDataInitializer
            )?.let {
                sqlQueryList.add(it)
            }
        } else {
            Log.i("[$tableName] No data file found")
        }

        var i = 0
        do {
            val pageFilePath = getDataPath(assetsPath, tableNameOriginal, ++i)
            val pageEntitySqlQueriesFile = File(pageFilePath)

            Log.d("[$tableName] Reading data at path $pageFilePath")

            if (pageEntitySqlQueriesFile.exists()) {
                getSqlQueriesForTableFromFile(
                    tableName,
                    pageEntitySqlQueriesFile,
                    fields,
                    staticDataInitializer
                )?.let {
                    sqlQueryList.add(it)
                }
            } else {
                Log.i("[$tableName] No data file found")
            }
        } while (pageEntitySqlQueriesFile.exists())

        return sqlQueryList
    }

    private fun getSqlQueriesForTableFromFile(
        tableName: String,
        entitySqlQueriesFile: File,
        fields: List<Field>,
        staticDataInitializer: StaticDataInitializer
    ): SqlQuery? {

        val jsonString = entitySqlQueriesFile.readFile()

        if (jsonString.isNotEmpty()) {
            retrieveJSONObject(jsonString)?.let { jsonObj ->

                val entities = jsonObj.getSafeArray("__ENTITIES")

                jsonObj.getSafeInt("__GlobalStamp")?.let { globalStamp ->
                    if (!dumpedTables.contains(tableName)) dumpedTables.add(tableName)
                    if (globalStamp > initialGlobalStamp) initialGlobalStamp = globalStamp
                }

                entities?.let {

                    val sqlQueryBuilder = SqlQueryBuilder(it, fields)

                    relatedEntitiesMapList.add(sqlQueryBuilder.relatedEntitiesMap)
                    Log.d("sqlQueryBuilder.relatedEntitiesMap.size = ${sqlQueryBuilder.relatedEntitiesMap.size}")
                    Log.d("relatedEntitiesMapList.size = ${relatedEntitiesMapList.size}")
                    updateQueryHolderList.addAll(sqlQueryBuilder.updateQueryHolderList)

                    return getQueryFromSqlQueryBuilder(
                        sqlQueryBuilder,
                        tableName,
                        staticDataInitializer
                    )
                }
            }

            Log.i("[$tableName] Couldn't find entities to extract")

        } else {
            Log.i("[$tableName] Empty data file")
        }

        return null
    }

    private fun getQueryFromSqlQueryBuilder(
        sqlQueryBuilder: SqlQueryBuilder,
        tableName: String,
        staticDataInitializer: StaticDataInitializer
    ): SqlQuery {

        val propertyList = sqlQueryBuilder.hashMap.toSortedMap().keys.toList()
        propertyListMap[tableName] = propertyList

        Log.d("[$tableName] ${sqlQueryBuilder.outputEntities.size} entities extracted")

        return staticDataInitializer.getQuery(
            tableName,
            propertyList,
            sqlQueryBuilder.outputEntities
        )
    }
}
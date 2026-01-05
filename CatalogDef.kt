import org.json.JSONObject
import java.io.File

class CatalogDef(catalogFile: File) {

    lateinit var dataModelAliases: List<DataModelAlias>
    lateinit var baseCatalogDef: List<DataModelAlias>
    lateinit var relations: List<Relation>
    private lateinit var jsonObj: JSONObject

    init {
        val jsonString = catalogFile.readFile()
        Log.d(
            "==================================\n" +
                    "CatalogDef init\n" +
                    "==================================\n"
        )

        if (jsonString.isEmpty()) {
            throw Exception("Json file ${catalogFile.name} is empty")
        }

        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            baseCatalogDef = getCatalogDef(isBaseDef = true)
            dataModelAliases = getCatalogDef()
            relations = getCatalogRelations()
            Log.d("> DataModels list successfully read.")

        } ?: kotlin.run {
            Log.e("Could not read global json object from file ${catalogFile.name}")
        }
    }

    // If baseDef, we don't want to simplify relations with __fieldKey
    private fun getCatalogDef(isBaseDef: Boolean = false): List<DataModelAlias> {
        val dataModelAliases = mutableListOf<DataModelAlias>()
        val dataModels = jsonObj.getSafeObject("structure")?.getSafeArray("definition").getObjectListAsString()

        dataModels.forEach { dataModelString ->
            retrieveJSONObject(dataModelString)?.getDataModelCatalog(isBaseDef)?.let { dataModelCatalog ->
                dataModelAliases.add(dataModelCatalog)
            }
        }
        return dataModelAliases
    }

    private fun getCatalogRelations(): List<Relation> {
        val tabNames = listOf("Emp", "Serv", "Off")
        Log.d(" ===== RELATIONS =====")
        dataModelAliases.filter { it.name in tabNames }.forEach { dm ->
            Log.d("FIELDS --------")
            dm.fields.forEach { field ->
                Log.d("[${dm.name}] ${field.name}")
            }
            Log.d("RELATIONS --------")
            dm.relations.forEach { relation ->
                Log.d("[${dm.name}] ${relation.name} (${relation.path})")
            }
        }
        return dataModelAliases.flatMap { it.relations }
    }

    // If baseDef, we don't want to simplify relations with __fieldKey
    private fun JSONObject.getDataModelCatalog(isBaseDef: Boolean): DataModelAlias? {
        val name = this.getSafeString("name")
        val tableNumber = this.getSafeInt("tableNumber")
        if (name == null || tableNumber == null)
            return null

        val dataModel = DataModelAlias(tableNumber = tableNumber, name = name)

        val fields = mutableListOf<FieldCatalog>()
        val relations = mutableListOf<Relation>()

        this.getSafeArray("fields")?.getObjectListAsString()?.forEach { fieldString ->
            retrieveJSONObject(fieldString)?.getFieldCatalog(tableNumber.toString())?.let { field ->
                fields.add(field)
            }
        }

        val fieldsToRemove = mutableListOf<FieldCatalog>()
        val fieldsToAdd = mutableListOf<FieldCatalog>()

        if (!isBaseDef) {
            fields.forEach { field ->
                if (field.isRelation()) {
                    val subFields = baseCatalogDef.find { it.name == field.relatedDataClass }?.fields?.convertToFields()
                    field.createRelation(name, subFields)?.let { relation ->
                        relations.add(relation)
                        if (relation.type == RelationType.MANY_TO_ONE){
                            val relationKeyField = buildNewKeyFieldCatalog(relation.name, tableNumber.toString())
                            fieldsToAdd.add(relationKeyField)
                        }
                    }
                }
            }

            fields.removeAll(fieldsToRemove)
            fields.addAll(fieldsToAdd)
        }

        dataModel.fields = fields
        dataModel.relations = relations
        return dataModel
    }

    private fun JSONObject.getFieldCatalog(dataModelId: String): FieldCatalog? {
        val name = this.getSafeString("name") ?: return null
        val fieldCatalog = FieldCatalog(name = name, dataModelId = dataModelId)
        this.getSafeString("kind")?.let { fieldCatalog.kind = it }
        this.getSafeString("relatedDataClass")?.let { fieldCatalog.relatedDataClass = it }
        this.getSafeString("inverseName")?.let { fieldCatalog.inverseName = it }
        this.getSafeString("id")?.let { fieldCatalog.id = it } ?: kotlin.run { fieldCatalog.id = name }
        this.getSafeBoolean("isToOne")?.let { fieldCatalog.isToOne = it }
        this.getSafeBoolean("isToMany")?.let { fieldCatalog.isToMany = it }
        this.getSafeInt("fieldType")?.let { fieldCatalog.fieldType = it }
        this.getSafeInt("relatedTableNumber")?.let { fieldCatalog.relatedTableNumber = it }
        this.getSafeString("path")?.let { fieldCatalog.path = it }

        when {
            fieldCatalog.isToMany == true -> {
                fieldCatalog.fieldTypeString = "Entities<${fieldCatalog.relatedDataClass?.tableNameAdjustment()}>"
                fieldCatalog.isToOne = false
                fieldCatalog.variableType = VariableType.VAR.string
            }
            fieldCatalog.isToOne == true -> {
                fieldCatalog.fieldTypeString = fieldCatalog.relatedDataClass
                fieldCatalog.isToMany = false
                fieldCatalog.variableType = VariableType.VAR.string
            }
            else -> {
                fieldCatalog.fieldTypeString = typeStringFromTypeInt(fieldCatalog.fieldType)
                fieldCatalog.isToMany = false
                fieldCatalog.isToOne = false
            }
        }
        return fieldCatalog
    }
}

data class DataModelAlias(
    var tableNumber: Int,
    var name: String,
    var fields: MutableList<FieldCatalog> = mutableListOf(),
    var relations: MutableList<Relation> = mutableListOf()
)

data class FieldCatalog(
    var name: String,
    var dataModelId: String,
    var kind: String? = null,
    var relatedDataClass: String? = null,
    var fieldTypeString: String? = null,
    var isToOne: Boolean? = null,
    var isToMany: Boolean? = null,
    var fieldType: Int? = null,
    var inverseName: String? = null,
    var relatedTableNumber: Int? = null,
    var path: String? = null,
    var id: String? = null,
    var variableType: String = VariableType.VAL.string
) {

    fun isRelation() = kind == "relatedEntity" || kind == "relatedEntities"

    fun createRelation(currentTable: String, subFields: List<Field>?): Relation? {
        relatedDataClass?.let { dest ->
            inverseName?.let { inv ->
                return Relation(
                    source = currentTable,
                    target = dest,
                    name = name,
                    type = if (isToOne == true) RelationType.MANY_TO_ONE else RelationType.ONE_TO_MANY,
                    subFields = subFields ?: listOf(),
                    inverseName = inv,
                    path = path ?: "",
                    relation_embedded_return_type = dest
                )
            }
        }
        return null
    }

    fun convertToField(): Field {
        val field = Field(name = name)
        field.id = id
        field.fieldType = fieldType
        field.fieldTypeString = fieldTypeString
        field.relatedTableNumber = relatedTableNumber
        field.variableType = variableType
        field.kind = kind
        field.inverseName = inverseName
        when (isToMany) {
            true -> {
                field.isToMany = true
                field.relatedEntities = relatedDataClass
            }
            else -> field.isToMany = false
        }
        field.relatedDataClass = relatedDataClass
        field.dataModelId = dataModelId
        field.path = path
        return field
    }
}

fun List<FieldCatalog>.convertToFields(): List<Field> {
    val fields = mutableListOf<Field>()
    this.forEach { fieldCatalog ->
        fields.add(fieldCatalog.convertToField())
    }
    return fields
}

fun buildNewKeyFieldCatalog(name: String, dataModelId: String): FieldCatalog {
    val newKeyField =
        FieldCatalog(name = "__${name.validateWordDecapitalized()}Key", dataModelId = dataModelId)
    newKeyField.fieldType = 0
    newKeyField.fieldTypeString = STRING_TYPE
    newKeyField.variableType = VariableType.VAR.string
    return newKeyField
}
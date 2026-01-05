import org.json.JSONObject

fun JSONObject.getDataModelList(catalogDef: CatalogDef, isCreateDatabaseCommand: Boolean = false): List<DataModel> {
    val dataModelList = mutableListOf<DataModel>()
    val dataModels = if (isCreateDatabaseCommand)
        this.getSafeObject(DATAMODEL_KEY)
    else
        this.getSafeObject(PROJECT_KEY)?.getSafeObject(DATAMODEL_KEY)

    val fieldToAddList = mutableListOf<FieldToAdd>()

    val aliasToHandleList = mutableListOf<Field>()

    dataModels?.keys()?.forEach { keyDataModel ->

        val newDataModelJSONObject = dataModels.getSafeObject(keyDataModel.toString())

        newDataModelJSONObject?.getSafeObject(EMPTY_KEY)?.getSafeString(NAME_KEY)?.let { dataModelName ->

            // Remove any slave dataModel with same name added before, save its fields
            val savedFields = mutableListOf<Field>()
            val savedRelations = mutableListOf<Relation>()
            dataModelList.find { it.name == dataModelName }?.fields?.let {
                savedFields.addAll(it)
            }
            dataModelList.find { it.name == dataModelName }?.relations?.let {
                savedRelations.addAll(it)
            }
            if (dataModelList.removeIf { dataModel -> dataModel.name == dataModelName }) {
                Log.d("DataModel removed from list : $dataModelName")
            }

            val newDataModel = DataModel(id = keyDataModel.toString(), name = dataModelName, isSlave = false)
            Log.d("newDataModel.name : $dataModelName")
            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(LABEL_KEY)?.let { newDataModel.label = it }
            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(SHORTLABEL_KEY)
                ?.let { newDataModel.shortLabel = it }
            var missingIcon = true
            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(ICON_KEY)?.let { iconPath ->
                if (iconPath.contains(".")) {
                    newDataModel.iconPath = correctIconPath(iconPath)
                    missingIcon = false
                }
            }

            if (missingIcon) {
                newDataModel.iconPath = null
            }

            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeObject(FILTER_KEY)?.let {
                if (it.getSafeBoolean(VALIDATED_KEY) == true)
                    newDataModel.query = it.getSafeString(STRING_KEY)?.replace("\"", "'")
            }

            val fieldList = mutableListOf<Field>()
            val relationList = mutableListOf<Relation>()

            Log.d("Checking fields")

            newDataModelJSONObject.keys().forEach eachKeyField@{ keyField ->
                if (keyField !is String) return@eachKeyField
                if (keyField != EMPTY_KEY) {
                    val newFieldJSONObject: JSONObject? = newDataModelJSONObject.getSafeObject(keyField.toString())
                    val field: Field? = newFieldJSONObject?.getDataModelField(keyField, keyDataModel, dataModelName, catalogDef)

                    field?.let {
                        Log.d("field, level 0: $field")

                        val subFields: List<Field> = newFieldJSONObject.getSubFields(dataModelName, catalogDef)

                        if (field.kind == "alias") {
                            subFields.forEach { subField ->
                                subField.relatedTableNumber = field.relatedTableNumber
                                subField.dataModelId = field.relatedDataClass
                            }
                            field.subFieldsForAlias = subFields
                            Log.d("Subfields for alias [${field.name}] are $subFields")
                            aliasToHandleList.add(field)

                            fieldList.addWithSanity(it, dataModelName, catalogDef)
                            Log.d("Adding to aliasToHandleList, keyDataModel = $keyDataModel , $field")

                        } else {
                            it.isSlave = false
                            fieldList.addWithSanity(it, dataModelName, catalogDef)
                            getRelation(it, dataModelName, subFields)?.let { relation ->
                                Log.d("relation.name : ${relation.name}")
                                relationList.add(relation)

                                if (relation.type == RelationType.MANY_TO_ONE) {

                                    val relationKeyField = buildNewKeyField(relation.name)
                                    fieldList.add(relationKeyField)

                                } else {

                                    Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                                    it.inverseName?.let { inverseName ->
                                        val newField = Field(
                                            name = inverseName,
                                            inverseName = it.name,
                                            relatedDataClass = dataModelName,
                                            fieldTypeString = dataModelName,
                                            relatedTableNumber = keyDataModel.toString().toIntOrNull(),
                                            relatedEntities = null,
                                            variableType = VariableType.VAR.string,
                                            kind = "relatedEntity"
                                        )
                                        Log.d("newField.name: ${newField.name}")
                                        getRelation(newField, relation.target, listOf())?.let { newRelation ->
                                            Log.d("newRelation.name : ${newRelation.name}")
                                            val newKeyField = buildNewKeyField(newRelation.name)

                                            val fieldToAdd = FieldToAdd(relation.target, newField, newKeyField, newRelation)
                                            fieldToAddList.add(fieldToAdd)
                                        }
                                    }
                                }
                            }
                        }

                        Log.d("Check if there is a slave table to add")

                        newFieldJSONObject.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
                            newFieldJSONObject.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { relatedTableNumber ->
                                val slaveDataModel = DataModel(
                                    id = relatedTableNumber.toString(),
                                    name = relatedDataClass,
                                    isSlave = true
                                )
                                Log.d("slaveDataModel.name : $relatedDataClass")

                                val slaveFieldList = mutableListOf<Field>()
                                val slaveRelationList = mutableListOf<Relation>()

                                newFieldJSONObject.keys().forEach eachSlaveKeyField@{ slaveKeyField ->
                                    if (slaveKeyField !is String) return@eachSlaveKeyField

                                    val newSlaveFieldJSONObject = newFieldJSONObject.getSafeObject(slaveKeyField.toString())

                                    val slaveField = newSlaveFieldJSONObject?.getDataModelField(
                                        slaveKeyField,
                                        relatedTableNumber.toString(),
                                        relatedDataClass,
                                        catalogDef
                                    )

                                    Log.d("slaveField : $slaveField")
                                    Log.d("newSlaveFieldJSONObject : $newSlaveFieldJSONObject")
                                    val parentField = field

                                    slaveField?.let { field ->

                                        val slaveSubFieldsForRelation: List<Field> = newSlaveFieldJSONObject.getSubFields(relatedDataClass, catalogDef)

                                        if (field.kind == "alias") {
                                            slaveSubFieldsForRelation.forEach { subField ->
                                                subField.relatedTableNumber = field.relatedTableNumber
                                                subField.dataModelId = field.relatedDataClass
                                            }
                                            field.subFieldsForAlias = slaveSubFieldsForRelation
                                            Log.d("Subfields for alias [${field.name}] are $slaveSubFieldsForRelation")

                                            field.parentIfSlave = it
                                            aliasToHandleList.add(field)
                                            slaveFieldList.add(field)
                                            Log.d("Adding to aliasToHandleList, keyDataModel = $relatedTableNumber , $field")
                                        } else {
                                            field.isSlave = true
                                            slaveFieldList.addWithSanity(field, relatedDataClass, catalogDef)
                                            getRelation(field, relatedDataClass, slaveSubFieldsForRelation)?.let { relation ->

                                                Log.d("slave relation : $relation")
                                                slaveRelationList.add(relation)

                                                if (relation.type == RelationType.MANY_TO_ONE) {

                                                    val relationKeyField = buildNewKeyField(relation.name)
                                                    slaveFieldList.add(relationKeyField)

                                                } else {

                                                    Log.d("One to many slave relation, need to add the inverse many to one relation to its Entity definition")
                                                    it.inverseName?.let {
                                                        val newField = Field(
                                                            name = relation.inverseName,
                                                            inverseName = relation.name,
                                                            relatedDataClass = relatedDataClass,
                                                            fieldTypeString = relatedDataClass,
                                                            relatedTableNumber = relatedTableNumber,
                                                            relatedEntities = null,
                                                            variableType = VariableType.VAR.string,
                                                            kind = "relatedEntity"
                                                        )
                                                        Log.d("slave newField.name : ${newField.name}")
                                                        getRelation(
                                                            newField,
                                                            relation.target,
                                                            listOf()
                                                        )?.let { newRelation ->
                                                            Log.d("slave newRelation.name : ${newRelation.name}")
                                                            val newKeyField = buildNewKeyField(newRelation.name)

                                                            val fieldToAdd = FieldToAdd(
                                                                relation.target,
                                                                newField,
                                                                newKeyField,
                                                                newRelation
                                                            )
                                                            fieldToAddList.add(fieldToAdd)
                                                        }
                                                    }
                                                }
                                                Log.d("Slave relation, need to create a relation with relationAdjustment() (example: serviceEmployees for service.employees)")
                                                val parentPart = if (parentField.kind == "alias") parentField.path else parentField.name
                                                val childPart = if (field.kind == "alias") field.path else field.name
                                                val path =  "$parentPart.$childPart"
                                                val adjustedRelation = Relation(
                                                    source = dataModelName,
                                                    target = relation.target,
                                                    name = path.relationNameAdjustment(),
                                                    type = relation.type,
                                                    subFields = slaveSubFieldsForRelation,
                                                    inverseName = "",
                                                    path = path,
                                                    relation_embedded_return_type = buildRelationEmbeddedReturnType(catalogDef, dataModelName, path)
                                                )
                                                Log.d("adjustedRelation : $adjustedRelation")
                                                relationList.add(adjustedRelation)
                                            }
                                        }

                                        Log.d("Slave slave alias checking")
                                        Log.d("Slave slave alias checking, newSlaveFieldJSONObject : $newSlaveFieldJSONObject")

                                        newSlaveFieldJSONObject.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
                                            newSlaveFieldJSONObject.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { relatedTableNumber ->

                                                val slaveSlaveDataModel = DataModel(
                                                    id = relatedTableNumber.toString(),
                                                    name = relatedDataClass,
                                                    isSlave = true
                                                )
                                                Log.d("slaveSlaveDataModel.name : $relatedDataClass")

                                                val slaveSlaveFieldList = mutableListOf<Field>()
                                                val slaveSlaveRelationList = mutableListOf<Relation>()

                                                newSlaveFieldJSONObject.keys().forEach eachSlaveSlaveKeyField@{ slaveSlaveKeyField ->
                                                    if (slaveSlaveKeyField !is String) return@eachSlaveSlaveKeyField

                                                    Log.d("slaveSlaveKeyField: $slaveSlaveKeyField")

                                                    val newSlaveSlaveFieldJSONObject = newSlaveFieldJSONObject.getSafeObject(slaveSlaveKeyField.toString())

                                                    Log.d("newSlaveSlaveFieldJSONObject: $newSlaveSlaveFieldJSONObject")

                                                    val slaveSlaveField = newSlaveSlaveFieldJSONObject?.getDataModelField(
                                                        slaveSlaveKeyField,
                                                        relatedTableNumber.toString(),
                                                        relatedDataClass,
                                                        catalogDef
                                                    )

                                                    Log.d("slaveSlaveField: $slaveSlaveField")

                                                    slaveSlaveField?.let { slaveSlaveField ->
                                                        if (slaveSlaveField.kind == "alias") {
//                                                            slaveField.isSlave = true
                                                            slaveSlaveField.parentIfSlave = field
                                                            slaveSlaveField.grandParentsIfSlave = it
                                                            aliasToHandleList.add(slaveSlaveField)
                                                            slaveSlaveFieldList.add(slaveSlaveField)
                                                            Log.d("Adding to aliasToHandleList, keyDataModel = $relatedTableNumber , $slaveSlaveField")
                                                        } else {
                                                            // only scalar here
                                                            slaveSlaveFieldList.add(slaveSlaveField)
                                                        }

                                                        Log.d("Checking if we already added this dataModel")

                                                        val dataModelIndex =
                                                            dataModelList.indexOfFirst { dataModel -> dataModel.name == relatedDataClass } // -1 if not found

                                                        when {
                                                            dataModelIndex != -1 -> { // dataModel exists
                                                                slaveSlaveFieldList.forEach { slaveSlaveField ->
                                                                    if (dataModelList[dataModelIndex].fields?.find { field -> field.name == slaveSlaveField.name } == null) {
                                                                        dataModelList[dataModelIndex].fields?.add(slaveSlaveField)
                                                                    }
                                                                }
//                                                                slaveSlaveRelationList.forEach { slaveSlaveRelation ->
//                                                                    if (dataModelList[dataModelIndex].relations?.find { relation -> relation.name == slaveSlaveRelation.name } == null) {
//                                                                        dataModelList[dataModelIndex].relations?.add(slaveSlaveRelation)
//                                                                    }
//                                                                }
                                                            }
                                                            relatedDataClass == dataModelName -> { // current table has a relation of its own type
                                                                fieldList.addAll(slaveSlaveFieldList)
                                                            }
                                                            relatedDataClass == slaveDataModel.name -> { // current slave table has a relation of its own type
                                                                slaveFieldList.addAll(slaveSlaveFieldList)
                                                            }
                                                            else -> {
                                                                slaveSlaveDataModel.fields = slaveSlaveFieldList
                                                                slaveSlaveDataModel.relations = slaveSlaveRelationList
                                                                dataModelList.add(slaveSlaveDataModel)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Log.d("Checking if we already added this dataModel")

                                val dataModelIndex =
                                    dataModelList.indexOfFirst { dataModel -> dataModel.name == relatedDataClass } // -1 if not found

                                when {
                                    dataModelIndex != -1 -> { // dataModel exists
                                        slaveFieldList.forEach { slaveField ->
                                            if (dataModelList[dataModelIndex].fields?.find { field -> field.name == slaveField.name } == null) {
                                                dataModelList[dataModelIndex].fields?.add(slaveField)
                                            }
                                        }
                                        slaveRelationList.forEach { slaveRelation ->
                                            if (dataModelList[dataModelIndex].relations?.find { relation -> relation.name == slaveRelation.name } == null) {
                                                dataModelList[dataModelIndex].relations?.add(slaveRelation)
                                            }
                                        }
                                    }
                                    relatedDataClass == dataModelName -> { // current table has a relation of its own type
                                        savedFields.addAll(slaveFieldList)
                                        savedRelations.addAll(slaveRelationList)
                                    }
                                    else -> {
                                        slaveDataModel.fields = slaveFieldList
                                        slaveDataModel.relations = slaveRelationList
                                        dataModelList.add(slaveDataModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Log.d("Add savedFields from another definition of this table (in a relation)")
            savedFields.forEach { savedField ->
                if (!fieldList.map { it.name }.contains(savedField.name)) {
                    savedField.isSlave = true
                    fieldList.add(savedField)
                }
            }
            Log.d("Add savedRelations from another definition of this table (in a relation)")
            savedRelations.forEach { savedRelation ->
                if (!relationList.map { it.name }.contains(savedRelation.name)) {
                    relationList.add(savedRelation)
                }
            }

            newDataModel.fields = fieldList
            newDataModel.relations = relationList
            dataModelList.add(newDataModel)
        }
    }

    Log.d("Checking aliases")

    aliasToHandleList.forEach { aliasField ->
        Log.d("aliasField: $aliasField")

        aliasField.dataModelId?.let { dataModelId ->
            Log.d("dataModelId: $dataModelId")

            catalogDef.dataModelAliases.find { it.tableNumber.toString() == dataModelId }?.let { dataModelAlias ->
                Log.d("dataModelAlias name: ${dataModelAlias.name}, fields.names : ${dataModelAlias.fields.joinToString { it.name }}")

                dataModelAlias.fields.find { it.name == aliasField.name }?.let { catalogField ->
                    Log.d("catalogField: $catalogField")

                    val path = cleanPath(aliasField.path ?: "", dataModelAlias.name, catalogDef)
                    Log.d("cleaned path: $path")

                    var nextTableName: String? = dataModelAlias.name

                    path.split(".").forEach { relationName ->
                        Log.d("nextTableName: $nextTableName")
                        Log.d("relationName: $relationName")
                        nextTableName = dataModelList.handleAlias(nextTableName, relationName, catalogDef) { fieldToAdd ->
                            fieldToAddList.add(fieldToAdd)
                        }
                    }

                    val target = catalogField.relatedDataClass ?: typeStringFromTypeInt(catalogField.fieldType)
                    Log.d("target = $target")
                    val isNotNativeType = catalogDef.dataModelAliases.map { it.name }.contains(target)
                    Log.d("isNotNativeType = $isNotNativeType")

                    // Add the end field in the related dataModel (add Name in Service for serviceName whose path is service.Name)
                    if (!isNotNativeType) {
                        Log.d("A native type field should be added")
                        val dest = destBeforeField(catalogDef, dataModelAlias.name, catalogField.path)
                        Log.d("dest: $dest")

                        catalogField.path?.substringAfterLast(".")?.let { endFieldName ->
                            Log.d("endFieldName: $endFieldName")
                            dataModelList.createMissingFieldIfNeeded(dest, catalogDef, endFieldName)
                        }
                        Log.d("//////// repeat with unAliased path to create unknown fields")
                        val unAliasedPath = unAliasPath(catalogField.path, dataModelAlias.name, catalogDef)
                        Log.d("unAliasedPath = $unAliasedPath")
                        val destBeforeField = destBeforeField(catalogDef, dataModelAlias.name, unAliasedPath)
                        Log.d("destBeforeField: $destBeforeField")
                        val endFieldName = unAliasedPath.substringAfterLast(".")
                        Log.d("endFieldName: $endFieldName")

                        dataModelList.createMissingFieldIfNeeded(destBeforeField, catalogDef, endFieldName)
                    }

                    // if the alias is a slave, we need to create a relation from parent. Example: service.managerServiceName we must create serviceManagerService
                    val parent = aliasField.parentIfSlave
                    Log.d("parent = $parent")
                    // same with sub slaves / grandparent. Example : service.manager.ServiceName we must create serviceManagerService
                    val grandParent = aliasField.grandParentsIfSlave
                    Log.d("grandParent = $grandParent")

                    var relationsToCreate: MutableList<Relation>? = null

                    when {
                        grandParent != null && parent != null -> {
                            catalogDef.dataModelAliases.find { it.tableNumber.toString() == grandParent.dataModelId }?.let { dmAlias ->

                                val unAliasedPath = unAliasPath(grandParent.name + "." + parent.name + "." +catalogField.path, dmAlias.name, catalogDef)
                                relationsToCreate = getRelationsToCreate(catalogDef, dmAlias.name, if (isNotNativeType) unAliasedPath else unAliasedPath.substringBeforeLast("."))
                            }
                        }
                        parent != null -> {
                            catalogDef.dataModelAliases.find { it.tableNumber.toString() == parent.dataModelId }?.let { dmAlias ->

                                val unAliasedPath = unAliasPath(parent.name + "." +catalogField.path, dmAlias.name, catalogDef)
                                relationsToCreate = getRelationsToCreate(catalogDef, dmAlias.name, if (isNotNativeType) unAliasedPath else unAliasedPath.substringBeforeLast("."))
                            }
                        }
                        else -> {
//                            val unAliasedPath = unAliasPath(catalogField.path, dataModelAlias.name, catalogDef)
                            val unAliasedPath = unAliasPath(aliasField.path, dataModelAlias.name, catalogDef)
                            relationsToCreate = getRelationsToCreate(catalogDef, dataModelAlias.name, if (isNotNativeType) unAliasedPath else unAliasedPath.substringBeforeLast("."))
                        }
                    }

                    // Add each relation to the appropriate dataModel
                    relationsToCreate?.forEach { relation ->
                        val dmRelations = dataModelList.find { it.name == relation.source }?.relations ?: mutableListOf()
                        dmRelations.add(relation)
                        dataModelList.find { it.name == relation.source }?.relations = dmRelations
                    }
                }
            }
        }
    }

    Log.d("Sanity check for missing DataModel definition")
    Log.d("Sanity fields")
    dataModelList.forEach { dataModel ->
        val sanityFieldList = mutableListOf<Field>()
        val fieldDone: MutableList<String> = mutableListOf()
        dataModel.fields?.forEach eachDmField@{ field ->
            Log.d("field : $field")
            val nbNotSlave = dataModel.fields?.count { it.name == field.name && it.isSlave == false } ?: 0
            val nbSlaveField = dataModel.fields?.count { it.name == field.name && it.isSlave == true } ?: 0
            when {
                fieldDone.contains(field.name) -> return@eachDmField
                nbNotSlave == 1 -> {
                    if (field.isSlave != false) {
                        return@eachDmField
                    }
                }
                nbNotSlave == 0 && nbSlaveField == 1 -> {
                    if (field.isSlave != true) {
                        return@eachDmField
                    }
                }
            }
            Log.d("adding field : $field")
            fieldDone.add(field.name)
            field.relatedTableNumber?.let { relatedTableNumber -> // relation
                if (catalogDef.dataModelAliases.map { it.tableNumber.toString() }.contains(relatedTableNumber.toString())) {

                    sanityFieldList.add(field)
                } else {
                    Log.d("Excluding unknown field $field")
                }
            } ?: kotlin.run { // not relation
                sanityFieldList.add(field)
            }
        }
        Log.d("DataModel ${dataModel.name}, previous fields size = ${dataModel.fields?.size}, new size = ${sanityFieldList.size}")
        dataModel.fields = sanityFieldList

        Log.d("Sanity relations")
        val sanityRelationList = mutableListOf<Relation>()
        dataModel.relations?.forEach { relation ->
            Log.d("relation: $relation")
            if (catalogDef.dataModelAliases.map { it.name }.contains(relation.target) || relation.path.isNotEmpty()) {

                val sanitySubFieldList = mutableListOf<Field>()
                relation.subFields.forEach { subField ->
                    subField.relatedTableNumber?.let { relatedTableNumber -> // relation
                        if (catalogDef.dataModelAliases.map { it.tableNumber.toString() }.contains(relatedTableNumber.toString())) {

                            sanitySubFieldList.add(subField)
                        } else {
                            Log.d("Excluding unknown subField $subField")
                        }
                    } ?: kotlin.run { // not relation
                        sanitySubFieldList.add(subField)
                    }
                }
                Log.d("Relation ${relation.name} from dataModel ${dataModel.name}, previous subField size = ${relation.subFields.size}, new size = ${sanitySubFieldList.size}")
                relation.subFields = sanitySubFieldList

                sanityRelationList.add(relation)
            } else {
                Log.d("Excluding unknown relation $relation")
            }
        }
        Log.d("dataModel ${dataModel.name}, previous relationList size = ${dataModel.relations?.size}, new size = ${sanityRelationList.size}")
        dataModel.relations = sanityRelationList.distinct().toMutableList()
    }

    fieldToAddList.forEach { fieldToAdd ->
        Log.d("Field.name to add if not present: $fieldToAdd")
        dataModelList.find { it.name == fieldToAdd.targetTable }?.let { dm ->
            dm.fields?.let { dmFields ->
                if (!dmFields.map { it.name }.contains(fieldToAdd.field.name)) {
                    dmFields.add(fieldToAdd.field)
                }
                if (!dmFields.map { it.name }.contains(fieldToAdd.keyField.name)) {
                    dmFields.add(fieldToAdd.keyField)
                }
            }
            dm.relations?.let { dmRelations ->
                if (!dmRelations.map { it.name }.contains(fieldToAdd.relation.name)) {
                    dmRelations.add(fieldToAdd.relation)
                }
            }
        }
    }

    Log.d("Reordering fields")
    dataModelList.forEach {
        it.reOrderFields()
    }

    dataModelList.logDataModel()

    return dataModelList
}

fun List<DataModel>.logDataModel() {
    this.forEach { dm ->
        Log.d("\n\nDM name : ${dm.name}")
        Log.d("\n- FIELDS -------------------")
        dm.fields?.forEach { field ->
            Log.d("[${field.name}] : $field")
        }
        Log.d("\n- RELATIONS -------------------")
        dm.relations?.forEach { relation ->
            Log.d("[${relation.name}] : $relation")
        }
        Log.d("\n/////////////////////////////////////////////////////////////////////////////////\n\n\n")
    }
}

/**
 * Function to put relations at the end of the field lists
 */
fun DataModel.reOrderFields() {
    this.fields?.sortWith(compareBy { it.name.startsWith("__") && it.name.endsWith("Key") })
    this.fields?.sortWith(nullsLast(compareBy { it.inverseName }))
}

fun MutableList<Field>.addWithSanity(field: Field, tableName: String, catalogDef: CatalogDef) {
    if (catalogDef.dataModelAliases.find { it.name == tableName }?.fields?.map { it.name }?.contains(field.name) == true) {
        this.add(field)
    } else {
        Log.d("addWithSanity is not adding field named ${field.name} in table $tableName")
    }
}

// Also removes field extension
fun cleanPath(path: String?, source: String, catalogDef: CatalogDef): String {
    var nextTableName = source
    var newPath = ""
    path?.split(".")?.forEach eachPathPart@{
        Log.d("cleanPath: pathPart: $it")
        val pair = checkPath(it, nextTableName, catalogDef)
        Log.d("cleanPath: pair: $pair")
        nextTableName = pair.first ?: return@eachPathPart
        newPath = if (newPath.isEmpty())
            pair.second
        else
            newPath + "." + pair.second
    }
    return newPath.removeSuffix(".")
}

fun MutableList<DataModel>.handleAlias(nextTableName: String?, relationName: String, catalogDef: CatalogDef, fieldToAddCallback: (fieldToAdd: FieldToAdd) -> Unit): String? {

    var returnNextTableName: String? = null
    val sourceDM: DataModel? = this.find { it.name == nextTableName }
    if (sourceDM == null) {
        Log.d("sourceDM $nextTableName doesn't exist")

        // dataModel does not exists, create one from catalog def
        catalogDef.baseCatalogDef.find { it.name == nextTableName }?.let { dmAlias ->
            Log.d("dmAlias: $dmAlias")
            dmAlias.fields.find { it.name == relationName }?.let { fieldCatalog -> // Name
                Log.d("fieldCatalog: $fieldCatalog")
                val newDM = DataModel(id = dmAlias.tableNumber.toString(), name = dmAlias.name, isSlave = true)
                val field = fieldCatalog.convertToField()
                val fieldList = mutableListOf<Field>()

                if (fieldCatalog.kind == null) {
                    fieldList.add(field)
                } else {
                    getRelation(field, dmAlias.name, listOf())?.let { relation ->
                        returnNextTableName = relation.target
                        Log.d("relation.name : ${relation.name}")
                        if (newDM.relations?.find { it.name == relation.name && it.source == relation.source } == null)
                            newDM.relations = mutableListOf(relation)

                        // Add target DM if doesn't exists
                        if (this.find { it.name == relation.target } == null) {
                            val newTargetDM = DataModel(id = field.relatedTableNumber.toString(), name = relation.target, isSlave = true)
                            this.add(newTargetDM)
                            Log.d("target DM [${relation.target}] doesn't exists, adding it !")
                        }

                        if (relation.type == RelationType.MANY_TO_ONE) {
                            val relationKeyField = buildNewKeyField(relation.name)
                            fieldList.add(relationKeyField)
                        } else {

                            Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                            field.inverseName?.let { inverseName ->
                                val newField = Field(
                                    name = inverseName,
                                    inverseName = field.name,
                                    relatedDataClass = dmAlias.name,
                                    fieldTypeString = dmAlias.name,
                                    relatedTableNumber = dmAlias.tableNumber,
                                    relatedEntities = null,
                                    variableType = VariableType.VAR.string,
                                    kind = "relatedEntity"
                                )
                                Log.d("newField.name: ${newField.name}")
                                getRelation(newField, relation.target, listOf())?.let { newRelation ->
                                    Log.d("newRelation.name : ${newRelation.name}")
                                    val newKeyField = buildNewKeyField(newRelation.name)

                                    val fieldToAdd =
                                        FieldToAdd(relation.target, newField, newKeyField, newRelation)
                                    fieldToAddCallback(fieldToAdd)
                                }
                            }
                        }
                    }
                }
                newDM.fields = fieldList
                this.add(newDM)
            }
        }
    } else {
        Log.d("sourceDM $nextTableName already exists")
        // dataModel exists
        catalogDef.baseCatalogDef.find { it.name == nextTableName }?.let { dmAlias ->
            Log.d("dmAlias: $dmAlias")
            dmAlias.fields.find { it.name == relationName }?.let { fieldCatalog ->
                Log.d("fieldCatalog: $fieldCatalog")
                val newList = sourceDM.fields?.toMutableList() ?: mutableListOf()
                val field = fieldCatalog.convertToField()

                if (fieldCatalog.kind == null) {
                    newList.add(field)
                } else {
                    getRelation(field, dmAlias.name, listOf())?.let { relation ->
                        field.variableType = VariableType.VAR.string
                        returnNextTableName = relation.target
                        Log.d("relation.name : ${relation.name}")
                        if (sourceDM.relations?.find { it.name == relation.name && it.source == relation.source } == null) {
                            if (sourceDM.relations == null) {
                                sourceDM.relations = mutableListOf(relation)
                            } else {
                                sourceDM.relations?.add(relation)
                            }
                        }

                        // Add target DM if doesn't exists
                        if (this.find { it.name == relation.target } == null) {
                            val newTargetDM = DataModel(id = field.relatedTableNumber.toString(), name = relation.target, isSlave = true)
                            this.add(newTargetDM)
                            Log.d("target DM [${relation.target}] doesn't exists, adding it !")
                        }

                        if (relation.type == RelationType.MANY_TO_ONE) {
                            val relationKeyField = buildNewKeyField(relation.name)
                            newList.takeIf { newList.find { it.name == relationKeyField.name } == null }?.add(relationKeyField)
                            newList.add(field)
                        } else {
                            newList.add(field)
                            Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                            field.inverseName?.let { inverseName ->
                                val newField = Field(
                                    name = inverseName,
                                    inverseName = field.name,
                                    relatedDataClass = dmAlias.name,
                                    fieldTypeString = dmAlias.name,
                                    relatedTableNumber = dmAlias.tableNumber,
                                    relatedEntities = null,
                                    variableType = VariableType.VAR.string,
                                    kind = "relatedEntity"
                                )
                                Log.d("newField.name: ${newField.name}")
                                getRelation(newField, relation.target, listOf())?.let { newRelation ->
                                    Log.d("newRelation.name : ${newRelation.name}")
                                    val newKeyField = buildNewKeyField(newRelation.name)

                                    val fieldToAdd =
                                        FieldToAdd(relation.target, newField, newKeyField, newRelation)
                                    fieldToAddCallback(fieldToAdd)
                                }
                            }
                        }
                    }
                }
                sourceDM.fields = newList
            }
        }
    }
    return returnNextTableName
}

fun MutableList<DataModel>.createMissingFieldIfNeeded(dest: String, catalogDef: CatalogDef, endFieldName: String) {
    catalogDef.dataModelAliases.find { it.name == dest }?.fields?.find { it.name == endFieldName }?.let { endField ->
        Log.d("endField: $endField")

        val newList = this.find { it.name == dest }?.fields ?: mutableListOf()
        newList.takeIf { newList.find { it.name == endField.name } == null }?.add(endField.convertToField())
        Log.d("endField.path = ${endField.path}")
        // If endField is an alias, also add the path field
        if (!endField.path.isNullOrEmpty()) {
            catalogDef.dataModelAliases.find { it.name == dest }?.fields?.find { it.name == endField.path }?.let { pathField ->
                Log.d("pathField: $pathField")
                newList.takeIf { newList.find { it.name == pathField.name } == null }?.add(pathField.convertToField())
            }
        }
        this.find { it.name == dest }?.fields = newList
    }
}

fun JSONObject?.getDataModelField(keyField: String, dataModelId: String?, dataModelName: String, catalogDef: CatalogDef): Field {
    Log.d("getDataModelField: THIS = $this")
    val field = Field(name = "")
    this?.getSafeString(LABEL_KEY)?.let { field.label = it }
    this?.getSafeString(SHORTLABEL_KEY)?.let { field.shortLabel = it }
    this?.getSafeInt(FIELDTYPE_KEY)?.let { field.fieldType = it }
    this?.getSafeString(VALUE_TYPE_KEY)?.let { field.valueType = it }
    this?.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { field.relatedTableNumber = it }
    this?.getSafeString(INVERSENAME_KEY)?.let { field.inverseName = it }
    this?.getSafeString(NAME_KEY)?.let { fieldName -> // BASIC FIELD
        field.name = fieldName
        field.id = keyField
        field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
    }
    this?.getSafeString(KIND_KEY)?.let {
        field.kind = it
        if (field.kind == "alias" || field.kind == "calculated") {
            field.name = keyField
            field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
        }
    }
    this?.getSafeString(FORMAT_KEY)?.let {
        field.format = it
    }
    this?.getSafeString(ICON_KEY)?.let { iconPath -> // useful when copied to an empty list / detail form
        if (iconPath.contains(".")) {
            field.icon = correctIconPath(iconPath)
        }
    }
    this?.getSafeBoolean(ISTOMANY_KEY)?.let { isToMany -> // Slave table defined in another table will have isToMany key
        field.name = keyField
        field.variableType = VariableType.VAR.string
        this.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
            if (isToMany) {
                field.isToMany = true
                field.relatedEntities = relatedDataClass
                field.fieldTypeString = "Entities<${relatedDataClass.tableNameAdjustment()}>"
            } else {
                field.isToMany = false
                field.relatedDataClass = relatedDataClass
                field.fieldTypeString = relatedDataClass
            }
        }
    } ?: kotlin.run {
        this?.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass -> // Many-to-one relation
            field.isToMany = false
            field.name = keyField
            field.relatedDataClass = relatedDataClass
            field.fieldTypeString = relatedDataClass
            field.variableType = VariableType.VAR.string
        }
        this?.getSafeString(RELATEDENTITIES_KEY)?.let { relatedEntities -> // One-to-many relation
            field.isToMany = true
            field.name = keyField
            field.relatedEntities = relatedEntities
            field.fieldTypeString = "Entities<${relatedEntities.tableNameAdjustment()}>"
            field.variableType = VariableType.VAR.string
        }
    }
    dataModelId?.let { field.dataModelId = it }
    this?.getSafeString("path")?.let { path ->
        if (field.kind == "alias") {
            val unAliasedPath = unAliasPath(path, dataModelName, catalogDef)
            field.path = unAliasedPath

            Log.d("dataModel field creation, path : $path, unaliased path : $unAliasedPath")

            if (path == unAliasedPath && !unAliasedPath.contains(".")) { // path : FirstName, name : First

                // path doesn't contains "." so it can't be relation.object of type 38 Object
                // so if it's type 38 object, it's a relation

                // if is N-1 relation
                val catalogRelation: Relation? = catalogDef.dataModelAliases.find { it.name == dataModelName }?.relations?.find { it.name == unAliasedPath }
                Log.d("catalogRelation = $catalogRelation")
                catalogRelation?.let { relation ->

                    field.relatedDataClass = relation.target
                    field.inverseName = relation.inverseName
                    field.fieldType = null
                    field.variableType = VariableType.VAR.string

                    if (relation.type == RelationType.MANY_TO_ONE) {
                        field.fieldTypeString = relation.target
                        field.isToMany = false
                    } else {
                        field.fieldTypeString = "Entities<${relation.target.tableNameAdjustment()}>"
                        field.isToMany = true
                    }
                }
            }

            if (field.fieldTypeString.isNullOrEmpty()) {
                val relationType = getRelationType(catalogDef, dataModelName, unAliasedPath)
                val dest = destWithField(catalogDef, dataModelName, unAliasedPath)
                field.fieldTypeString = if (relationType == RelationType.ONE_TO_MANY)
                    "Entities<$dest>"
                else
                    dest
            }
        }
    }
    if (field.label.isNullOrEmpty())
        field.label = field.name
    if (field.shortLabel.isNullOrEmpty())
        field.shortLabel = field.name

    Log.d("field:::::: $field")
    return field
}

fun JSONObject?.getSubFields(dataModelName: String, catalogDef: CatalogDef): List<Field> {
    val subList = mutableListOf<Field>()
    this?.let {
        this.keys().forEach { key ->
            val aSubFieldJsonObject: JSONObject? = this.getSafeObject(key.toString())
            aSubFieldJsonObject?.getDataModelField(key.toString(), null, dataModelName, catalogDef)?.let {
                it.isSlave = true
                if (it.kind == "relatedEntity" || it.kind == "relatedEntities" || it.kind == "alias") {
                    it.relatedDataClass?.let { relatedDataClass ->
                        val subFields = aSubFieldJsonObject.getSubFields(relatedDataClass, catalogDef)
                        it.subFieldsForAlias = subFields
                    }
                }
                subList.add(it)
            }
        }
    }
    return subList
}

fun buildNewKeyField(name: String): Field {
    val newKeyField =
        Field(name = "__${name.validateWordDecapitalized()}Key")
    Log.d("Many to One relation $name, adding new keyField.name : ${newKeyField.name}")
    newKeyField.fieldType = 0
    newKeyField.fieldTypeString = STRING_TYPE
    newKeyField.variableType = VariableType.VAR.string
    return newKeyField
}

data class FieldToAdd(
    val targetTable: String, val field: Field, val keyField: Field, val relation: Relation
)
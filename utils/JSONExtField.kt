import org.json.JSONObject

fun getFormFields(fieldList: List<String>, dataModelName: String, catalogDef: CatalogDef): List<Field> {
    val fields = mutableListOf<Field>()
    fieldList.forEach { fieldString ->
        fields.add(retrieveJSONObject(fieldString).getFormField(dataModelName, catalogDef))
    }
    return fields
}

fun JSONObject?.getFormField(dataModelName: String, catalogDef: CatalogDef): Field {
    val field = Field(name = "")
//    this?.getSafeString(LABEL_KEY)?.let { field.label = it }
//    this?.getSafeString(SHORTLABEL_KEY)?.let { field.shortLabel = it }
    this?.getSafeInt(FIELDTYPE_KEY)?.let { field.fieldType = it }
//    this?.getSafeInt(ID_KEY).let { field.id = it.toString() }
    this?.getSafeString(INVERSENAME_KEY)?.let { field.inverseName = it }
    this?.getSafeString(NAME_KEY)?.let {
        field.name = it
        field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
    }
    this?.getSafeString(KIND_KEY)?.let { field.kind = it }
//    this?.getSafeString(FORMAT_KEY)?.let { field.format = it }
//    this?.getSafeString(ICON_KEY)?.let { iconPath ->
//        if (iconPath.contains(".")) {
//            field.icon = correctIconPath(iconPath)
//        }
//    }
    this?.getSafeString(RELATEDDATACLASS_KEY)?.let {
        field.relatedDataClass = it
        field.fieldTypeString = it
    }
    this?.getSafeInt(RELATEDTABLENUMBER_KEY)?.let {
        field.relatedTableNumber = it
    }
//    this?.getSafeString(RELATEDENTITIES_KEY).let {
//        field.relatedEntities = it
//        field.fieldTypeString = "Entities<${it?.tableNameAdjustment()}>"
//    }

    this?.getSafeString("path")?.let { path ->

        val unAliasedPath = unAliasPath(path, dataModelName, catalogDef)
        field.path = unAliasedPath
        Log.d("Form field creation, path : $path, unaliased path : $unAliasedPath")
        Log.d("getFormField YY : json : $this")
        if (path == unAliasedPath && !unAliasedPath.contains(".")) { // path : FirstName, name : First

            // path doesn't contains "." so it can't be relation.object of type 38 Object
            // so if it's type 38 object, it's a relation

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


        // TODO: TO CHECK
        if (/*path != unAliasedPath &&*/ path.contains(".") && field.kind == "relatedEntities") {
            Log.d("field has his name being changed to path value")
            field.name = path
        }
    }
    Log.d("form field extracted: $field")
    return field
}


import java.io.File

data class Field(
        var id: String? = null,
        var name: String,
        var label: String? = null,
        var shortLabel: String? = null,
        var fieldType: Int? = null,
        var valueType: String? = null,
        var fieldTypeString: String? = null,
        var relatedEntities: String? = null,
        var relatedTableNumber: Int? = null,
        var inverseName: String? = null,
        var relatedDataClass: String? = null,
        var variableType: String = VariableType.VAL.string,
        var isToMany: Boolean? = null,
        var isSlave: Boolean? = null,
        var format: String? = null,
        var icon: String? = null,
        var kind: String? = null,
        var dataModelId: String? = null,
        var path: String? = null,
        var subFieldsForAlias: List<Field>? = null,
        var parentIfSlave: Field? = null,
        var grandParentsIfSlave: Field? = null
)

fun isPrivateRelationField(fieldName: String): Boolean = fieldName.startsWith("__") && fieldName.endsWith("Key")

fun Field.isImage() = this.fieldType == 3

fun Field.getImageFieldName() = this.path?.substringAfterLast(".") ?: ""

fun Field.getFieldKeyAccessor(dataModelList: List<DataModel>): String {
    if (this.isFieldAlias(dataModelList)) {
        Log.d("getFieldKeyAccessor, aliasField here, field is $this")
        val path = this.path ?: ""
        if (path.contains(".")) {
            var name = ""
            var nextPath = path.substringBeforeLast(".")
            while (nextPath.contains(".")) {

                name += nextPath.relationNameAdjustment() + "."
                Log.d("building name = $name")

                nextPath = nextPath.substringAfter(".")
            }
            val returnName = name + nextPath.relationNameAdjustment() + "." + path.substringAfterLast(".").fieldAdjustment()
            Log.d("getFieldKeyAccessor returnName: $returnName")
            return returnName.substringBeforeLast(".") + ".__KEY"
        } else {
            return "__KEY"
        }
    } else {
        return "__KEY"
    }
}

fun Field.getLayoutVariableAccessor(): String {
    Log.d("getLayoutVariableAccessor: this = $this")
    return if (this.name.fieldAdjustment().contains(".") || (this.kind == "alias" && this.path?.contains(".") == true))
        "entityData."
    else
        "entityData.__entity."
}

fun Field.isNotNativeType(dataModelList: List<DataModel>): Boolean = dataModelList.map { it.name }.contains(this.fieldTypeString) || this.fieldTypeString?.startsWith("Entities<") == true

fun Field.isNativeType(dataModelList: List<DataModel>): Boolean = this.isNotNativeType(dataModelList).not()

fun Field.getLabel(): String {
    return label?.encode() ?: ""
}

fun Field.getShortLabel(): String {
    return shortLabel?.encode() ?: ""
}

fun getIcon(dataModelList: List<DataModel>, form: Form, formField: Field): String {
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, formField) ?: return ""
    Log.d("getIcon, formField $formField")
    Log.d("getIcon, fieldFromDataModel $fieldFromDataModel")
    if (fieldFromDataModel.icon.isNullOrEmpty()) {
        fieldFromDataModel.id = when {
            fieldFromDataModel.isSlave == true -> formField.name
            fieldFromDataModel.id == null -> fieldFromDataModel.name
            else -> fieldFromDataModel.id
        }
        fieldFromDataModel.id?.let { fieldFromDataModel.id = it.toLowerCase().replace("[^a-z0-9]+".toRegex(), "_") }
        val dmKey = dataModelList.find { it.name == form.dataModel.name }?.id
        // Getting first relation
        var relatedDmKey = ""
        fieldFromDataModel.relatedTableNumber?.let { relatedTableNumber ->
            relatedDmKey = relatedTableNumber.toString()
        } ?: run {
            formField.path?.substringBefore(".")?.let { firstPart ->
                findRelationFromPath(dataModelList, form.dataModel.name, firstPart)?.let { firstRelation ->
                    relatedDmKey = dataModelList.find { it.name == firstRelation.target }?.id ?: ""
                }
            }
        }
        return if (fieldFromDataModel.isSlave == true)
            "related_field_icon_${dmKey}_${relatedDmKey}_${fieldFromDataModel.id}"
        else
            "field_icon_${dmKey}_${fieldFromDataModel.id}"
    }
    return fieldFromDataModel.icon ?: ""
}


fun Field.isRelation(dataModelList: List<DataModel>): Boolean =
    !this.inverseName.isNullOrEmpty() || (this.kind == "alias" && !isFieldAlias(dataModelList) )

fun Field.isOneToManyRelation(dataModelList: List<DataModel>): Boolean =
    this.relatedEntities != null && this.isRelation(dataModelList)

fun Field.isManyToOneRelation(dataModelList: List<DataModel>): Boolean =
    this.relatedEntities == null && this.isRelation(dataModelList)

fun correctIconPath(iconPath: String): String {
    // removes extension
    val withoutExt = if (iconPath.contains(".")) {
        iconPath.substring(0, iconPath.lastIndexOf('.'))
    } else {
        iconPath
    }
    val correctedIconPath = withoutExt
        .replace(".+/".toRegex(), "")
        .removePrefix(File.separator)
        .toLowerCase()
        .replace("[^a-z0-9]+".toRegex(), "_")
    return correctedIconPath
}

fun getFormatNameForType(pathHelper: PathHelper, dataModelList: List<DataModel>, form: Form, formField: Field): String {
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, formField) ?: return ""
    Log.d("getFormatNameForType, [${fieldFromDataModel.name}] field: $fieldFromDataModel")
    val format = fieldFromDataModel.format
    if (format.equals("integer")) {
        return when (fieldFromDataModel.fieldType) {
            6 -> "boolInteger" // Boolean
            11 -> "timeInteger" // Time
            else -> "integer"
        }
    }
    if (format.isNullOrEmpty()) {
        if (fieldFromDataModel.kind == "alias" && fieldFromDataModel.path?.contains(".") == true)
            return ""
        return when (typeFromTypeInt(fieldFromDataModel.fieldType)) {
            BOOLEAN_TYPE -> "falseOrTrue"
            DATE_TYPE -> "mediumDate"
            TIME_TYPE -> "mediumTime"
            INT_TYPE -> "integer"
            FLOAT_TYPE -> "decimal"
            OBJECT_TYPE-> "yaml"
            else -> ""
        }
    } else {
        if (format.startsWith("/")) {

            val formatPath = pathHelper.getCustomFormatterPath(format)
            getManifestJSONContent(formatPath)?.let {

                val fieldMapping = getFieldMappingFormatter(it, format)
                return if (fieldMapping.isValidFormatter() || fieldMapping.isValidKotlinCustomDataFormatter()) {
                    format
                } else {
                    when (typeFromTypeInt(fieldFromDataModel.fieldType)) {
                        OBJECT_TYPE -> "yaml"
                        else -> ""
                    }
                }
            }
        }
        return format
    }
}

fun getDataModelField(dataModelList: List<DataModel>, dataModel: DataModel, field: Field): Field? {
    Log.d("getDataModelField [${field.name}], field: $field")
    val partCount = field.name.count { it == '.'}
    val dataModel = dataModelList.find { it.id == dataModel.id }
    if (field.name.contains(".")) {
        Log.d("getDataModelField field.name contains '.'")
        dataModel?.relations?.find { it.name == field.name.split(".")[0] }?.let { fieldInRelationList ->
            Log.d("fieldInRelationList: $fieldInRelationList")
             fieldInRelationList.subFields.find { it.name == field.name.split(".")[1] }?.let { son ->
                 Log.d("son: $son")
                 if (partCount > 1) {
                     son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                         Log.d("grandSon: $grandSon")
                         return grandSon
                     }
                 } else {
                     return son
                 }
             }
        }
        Log.d("getDataModelField [${field.name}] not found in relations, going to check in fields")
        dataModel?.fields?.find { it.name == field.name.split(".")[0] }?.let { fieldInFieldList ->
            Log.d("fieldInFieldList: $fieldInFieldList")
            fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
                Log.d("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                        Log.d("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        Log.d("getDataModelField [${field.name}] not found in fields, going to check fieldAliases in fields")
        dataModel?.fields?.find { it.path == field.name && field.kind == "alias" }?.let { aliasFieldInFieldList ->
            Log.d("aliasFieldInFieldList: $aliasFieldInFieldList")
            return aliasFieldInFieldList
        }

    } else {
        Log.d("getDataModelField field.name doesn't contain '.'")

        dataModel?.fields?.find { it.name == field.name }?.let { field ->
            Log.d("Found field with this name: $field")
            return field
        }

        val fieldPath = field.path ?: ""
        val pathPartCount = fieldPath.count { it == '.'}
        if (fieldPath.contains(".")) {         // TODO: TO CHECK
            Log.d("getDataModelField fieldPath contains '.'")
            dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
                Log.d("fieldInFieldList: $fieldInFieldList")
                fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
                    Log.d("son: $son")
                    if (pathPartCount > 1) {
                        son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                            Log.d("grandSon: $grandSon")
                            return grandSon
                        }
                    } else {
                        return son
                    }
                }
            }
        }
    }

    Log.d("getDataModelField returns null")
    return null
}

fun getDataModelFieldFromPath(dataModelList: List<DataModel>, fieldFromDataModel: Field, fieldPath: String): Field? {
    Log.d("getDataModelFieldFromPath fieldPath [$fieldPath]")
    val partCount = fieldPath.count { it == '.'}
    val dataModel = dataModelList.find { it.id == fieldFromDataModel.relatedTableNumber.toString() }
    if (fieldPath.contains(".")) {
        Log.d("getDataModelFieldFromPath field.name contains '.'")
        dataModel?.relations?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInRelationList ->
            Log.d("fieldInRelationList: $fieldInRelationList")
            fieldInRelationList.subFields.find { it.name == fieldPath.split(".")[1] }?.let { son ->
                Log.d("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == fieldPath.split(".")[2] }?.let { grandSon ->
                        Log.d("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        Log.d("getDataModelFieldFromPath [$fieldPath] not found in relations, going to check in fields")
        dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
            Log.d("fieldInFieldList: $fieldInFieldList")
            fieldInFieldList.subFieldsForAlias?.find { it.name == fieldPath.split(".")[1] }?.let { son ->
                Log.d("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == fieldPath.split(".")[2] }?.let { grandSon ->
                        Log.d("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        Log.d("getDataModelFieldFromPath [$fieldPath] not found in fields, going to check fieldAliases in fields")
        dataModel?.fields?.find { it.path == fieldPath }?.let { aliasFieldInFieldList ->
            Log.d("aliasFieldInFieldList: $aliasFieldInFieldList")
            return aliasFieldInFieldList
        }

    } else {
        Log.d("getDataModelField field.name doesn't contain '.'")

        dataModel?.fields?.find { it.name == fieldPath }?.let { field ->
            Log.d("Found field with this name: $field")
            return field
        }
//
//        val fieldPath = field.path ?: ""
//        val pathPartCount = fieldPath.count { it == '.'}
//        if (fieldPath.contains(".")) {         // TODO: TO CHECK
//            Log.d("getDataModelField fieldPath contains '.'")
//            dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
//                Log.d("fieldInFieldList: $fieldInFieldList")
//                fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
//                    Log.d("son: $son")
//                    if (pathPartCount > 1) {
//                        son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
//                            Log.d("grandSon: $grandSon")
//                            return grandSon
//                        }
//                    } else {
//                        return son
//                    }
//                }
//            }
//        }
    }

    Log.d("getDataModelField returns null")
    return null
}

/**
 * Returns true if it has %length% placeholder AND it is a 1-N relation
 */

fun hasLabelPercentPlaceholder(dataModelList: List<DataModel>, dataModel: DataModel, field: Field): Boolean {
    Log.d("hasLabelPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, field) ?: return false
    Log.d("fieldFromDataModel: $fieldFromDataModel")
    val hasPercentPlaceholder = hasPercentPlaceholder(fieldFromDataModel.getLabel(), dataModelList, dataModel, field, fieldFromDataModel)
    Log.d("hasPercentPlaceholder : $hasPercentPlaceholder")
    return hasPercentPlaceholder
}

fun hasShortLabelPercentPlaceholder(dataModelList: List<DataModel>, dataModel: DataModel, field: Field): Boolean {
    Log.d("hasShortLabelPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, field) ?: return false
    Log.d("fieldFromDataModel: $fieldFromDataModel")
    val hasPercentPlaceholder = hasPercentPlaceholder(fieldFromDataModel.getShortLabel(), dataModelList, dataModel, field, fieldFromDataModel)
    Log.d("hasPercentPlaceholder : $hasPercentPlaceholder")
    return hasPercentPlaceholder
}

private fun hasPercentPlaceholder(label: String, dataModelList: List<DataModel>, dataModel: DataModel, formField: Field, fieldFromDataModel: Field): Boolean {
    Log.d("hasPercentPlaceholder field [${fieldFromDataModel.name}] label [$label] ////")
    val hasLengthPlaceholder = hasLengthPlaceholder(label, dataModelList, fieldFromDataModel)
    Log.d("hasLengthPlaceholder $hasLengthPlaceholder")
    val hasFieldPlaceholder = hasFieldPlaceholder(label, dataModelList, dataModel, formField)
    Log.d("hasFieldPlaceholder $hasFieldPlaceholder")
    return hasLengthPlaceholder || hasFieldPlaceholder
}

private fun hasLengthPlaceholder(label: String, dataModelList: List<DataModel>, fieldFromDataModel: Field): Boolean {
    Log.d("hasLengthPlaceholder field [${fieldFromDataModel.name}] label [$label] ////")
    val isRelation = fieldFromDataModel.isRelation(dataModelList)
    Log.d("isRelation: $isRelation")
    if (!isRelation) return false
    val isOneToManyRelation = fieldFromDataModel.isOneToManyRelation(dataModelList)
    Log.d("isOneToManyRelation = $isOneToManyRelation")
    if (!isOneToManyRelation) return false
    return label.contains("%length%")
}

fun hasFieldPlaceholder(label: String, dataModelList: List<DataModel>, dataModel: DataModel, formField: Field): Boolean {
    Log.d("hasFieldPlaceholder field [${formField.name}] label [$label] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, formField) ?: return false
    Log.d("fieldFromDataModel : $fieldFromDataModel")
    val isRelation = fieldFromDataModel.isRelation(dataModelList)
    Log.d("isRelation: $isRelation")
    if (!isRelation) return false
    val isManyToOneRelation = fieldFromDataModel.isManyToOneRelation(dataModelList)
    Log.d("isManyToOneRelation = $isManyToOneRelation")
    if (!isManyToOneRelation) return false
    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    regex.findAll(label).forEach { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        Log.d("Regexed fieldName: $fieldName")
        // Verify that fieldName exists in source dataModel
        val fieldExists = getDataModelFieldFromPath(dataModelList, fieldFromDataModel, fieldName) != null
        Log.d("fieldExists: $fieldExists")
        return fieldExists
    }
    return false
}

fun getLabelWithPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field, catalogDef: CatalogDef): String {
    Log.d("getLabelWithPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, field) ?: return ""
    Log.d("fieldFromDataModel: $fieldFromDataModel")
    return replacePercentPlaceholder(fieldFromDataModel.getLabel(), dataModelList, form, field, catalogDef)
}

fun getShortLabelWithPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field, catalogDef: CatalogDef): String {
    Log.d("getShortLabelWithPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, field) ?: return ""
    Log.d("fieldFromDataModel: $fieldFromDataModel")
    return replacePercentPlaceholder(fieldFromDataModel.getShortLabel(), dataModelList, form, field, catalogDef)
}

fun getEntryRelation(dataModelList: List<DataModel>, source: String, formField: Field, catalogDef: CatalogDef): String {
    Log.d("getEntryRelation field [${formField.name}] ////")
    return if (formField.name.contains(".")) {
        val relation: Relation? = findRelation(dataModelList, source, formField)
        Log.d("relation = $relation")
        if (relation != null)
            "entityData." + relation.name.relationNameAdjustment() + "." + getPathToManyWithoutFirst(relation, catalogDef)
        else
            "entityData." + formField.getFieldAliasName(dataModelList)
    } else {
        "entityData." + formField.getFieldAliasName(dataModelList)
    }
}

private fun replacePercentPlaceholder(label: String, dataModelList: List<DataModel>, form: Form, field: Field, catalogDef: CatalogDef): String {
    Log.d("replacePercentPlaceholder field [${field.name}] label [$label] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, field) ?: return ""
    val hasLengthPlaceholder = hasLengthPlaceholder(label, dataModelList, fieldFromDataModel)
    val labelWithLength = if (hasLengthPlaceholder) {

        val labelWithSize = if (field.name.contains(".")) {
            val relation: Relation? = findRelation(dataModelList, form.dataModel.name, field)
            Log.d("relation = $relation")
            if (relation != null)
                "entityData." + relation.name.relationNameAdjustment() + "." + getPathToManyWithoutFirst(relation, catalogDef) + ".size"
            else
                "entityData." + field.getFieldAliasName(dataModelList) + ".size"

        } else {
            "entityData." + field.getFieldAliasName(dataModelList) + ".size"
        }
        label.replace("%length%", "\" + $labelWithSize + \"")
    } else {
        label
    }
    val hasFieldPlaceholder = hasFieldPlaceholder(label, dataModelList, form.dataModel, field)
    return if (hasFieldPlaceholder) {
        replaceFieldPlaceholder(labelWithLength, dataModelList, fieldFromDataModel)
    } else {
        cleanPrefixSuffix("\"$labelWithLength\"")
    }
}

private fun replaceFieldPlaceholder(label: String, dataModelList: List<DataModel>, fieldFromDataModel: Field): String {
    Log.d("replaceFieldPlaceholder field [${fieldFromDataModel.name}] label [$label] ////")
    val labelWithoutRemainingLength = label.replace("%length%", "")
    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    val newLabel = regex.replace(labelWithoutRemainingLength) { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        Log.d("Regexed fieldName: $fieldName")
        val fieldInLabel: Field? = getDataModelFieldFromPath(dataModelList, fieldFromDataModel, fieldName)
        Log.d("fieldInLabel: $fieldInLabel")
        if (fieldInLabel != null)
            "\" + entityData.${fieldFromDataModel.name.fieldAdjustment()}.${fieldName.fieldAdjustment()}.toString() + \""
        else
            ""
    }
    val labelWithField = "\"" + newLabel.removePrefix(" ").removeSuffix(" ") + "\""
    return cleanPrefixSuffix(labelWithField)
}

fun cleanPrefixSuffix(label: String): String {
    return label.removePrefix("\"\" + ").removeSuffix(" + \"\"").removePrefix("\" + ").removeSuffix(" + \"")
}

fun getNavbarTitle(dataModelList: List<DataModel>, form: Form, formField: Field, catalogDef: CatalogDef): String {
    return getNavbarTitle(dataModelList, form.dataModel, formField, catalogDef)
}

fun getNavbarTitle(dataModelList: List<DataModel>, dataModel: DataModel, formField: Field, catalogDef: CatalogDef): String {
    Log.d("getNavbarTitle - getNavbarTitle field [${formField.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, formField) ?: return ""
    Log.d("getNavbarTitle - fieldFromDataModel: $fieldFromDataModel")

    fieldFromDataModel.format?.let { format ->
        Log.d("getNavbarTitle - format: $format")

        val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
        val formatWithoutRemainingLength = format.replace("%length%", "")
        val navbarTitle = regex.replace(formatWithoutRemainingLength) { matchResult ->
            val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
            // Verify that fieldName exists in source dataModel
            Log.d("Regexed fieldName: $fieldName")
            val path = unAliasPath(fieldName, dataModel.name, catalogDef)
            Log.d("path = $path")
            val endFieldName = path.substringAfterLast(".").fieldAdjustment()
            val destBeforeField = destBeforeField(catalogDef, dataModel.name, path)
            val endField = dataModelList.find { it.name.tableNameAdjustment() == destBeforeField.tableNameAdjustment() }?.fields?.find { it.name.fieldAdjustment() == endFieldName }

            Log.d("endField: $endField")

            if (endField?.isNativeType(dataModelList) == true) {

                val fieldAliasName = endField.getFieldAliasName(dataModelList)
                Log.d("fieldAliasName = $fieldAliasName")

                when {
                    path.contains(".") -> {
                        val relation = findRelationFromPath(dataModelList, dataModel.name, path.substringBeforeLast("."))
                        if (relation != null) {
                            Log.d("Found relation with same path with path $relation")

                            val pathToOneWithoutFirst = getPathToOneWithoutFirst(relation, catalogDef)
                            if (pathToOneWithoutFirst.isNotEmpty())
                                "\${(roomEntity as ${dataModel.name.tableNameAdjustment()}RoomEntity?)?.${relation.name.relationNameAdjustment()}?.${pathToOneWithoutFirst}?.$fieldAliasName.toString()}"
                            else
                                "\${(roomEntity as ${dataModel.name.tableNameAdjustment()}RoomEntity?)?.${relation.name.relationNameAdjustment()}?.$fieldAliasName.toString()}"
                        } else {
                            Log.d("No relation found with path : ${path.substringBeforeLast(".")}")
                            fieldName
                        }
                    }

                    else -> {
                        "\${(roomEntity as ${dataModel.name.tableNameAdjustment()}RoomEntity?)?.__entity?.${fieldAliasName.fieldAdjustment()}.toString()}"
                    }
                }
            } else {
                fieldName
            }
        }
        Log.d("navbarTitle: $navbarTitle")
        return navbarTitle
    }

    Log.d("fieldFromDataModel.format = null")
    return dataModelList.find { it.name == fieldFromDataModel.relatedEntities }?.label
            ?: dataModelList.find { it.name == fieldFromDataModel.relatedDataClass }?.label
            ?: ""
}

/**
 * fieldFromDataModel is here to get the Field from dataModel instead of list/detail form as some information may be missing.
 * If it's a related field, fieldFromDataModel will be null, therefore check the field variable
*/

fun getShortLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form.dataModel, field)
    return fieldFromDataModel?.getShortLabel() ?: ""
}

fun getLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form.dataModel, field)
    return fieldFromDataModel?.getLabel() ?: ""
}

fun isRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form.dataModel, field)
    return fieldFromDataModel?.isRelation(dataModelList) ?: false
}

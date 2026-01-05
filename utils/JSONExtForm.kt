import org.json.JSONObject

fun JSONObject.getFormList(dataModelList: List<DataModel>, formType: FormType, navigationTableList: List<String>, catalogDef: CatalogDef): List<Form> {
    val formList = mutableListOf<Form>()
    val formTypeKey = if (formType == FormType.LIST) LIST_KEY else DETAIL_KEY
    val forms = this.getSafeObject(PROJECT_KEY)?.getSafeObject(formTypeKey)

    Log.d("navigationTableList = ${navigationTableList.joinToString()}")

    forms?.keys()?.forEach { keyDataModel ->
        dataModelList.filter { it.isSlave == false }.find { it.id == keyDataModel }?.let { dataModel ->
            val form = Form(dataModel = dataModel)
            val newFormJSONObject = forms.getSafeObject(keyDataModel.toString())
            newFormJSONObject?.getSafeString(FORM_KEY)?.let {
                form.name = it
            }
            newFormJSONObject?.getSafeBoolean(SEARCHABLE_WITH_BARCODE)?.let {
                form.searchableWithBarcode = it
            }
            val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getObjectListAsString()
            form.fields = getFormFields(fieldList, dataModel.name, catalogDef)
            formList.add(form)
        }
    }

    dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
        if (navigationTableList.contains(dataModel.id) && !formList.map { it.dataModel.id }.contains(dataModel.id)) {
            val form = Form(dataModel = dataModel)
            formList.add(form)
        }
    }

    for (form in formList) {
        if (form.name.isNullOrEmpty()) {
            // Set default forms
            val fields = mutableListOf<Field>()
            dataModelList.find { it.name == form.dataModel.name }?.fields?.forEach {
                if (!isPrivateRelationField(it.name) && it.isSlave == false && it.label != null) {
                    // if Simple Table (default list form, avoid photo and relation)
                    if (formType == FormType.LIST && (it.fieldTypeString == PHOTO_TYPE || it.inverseName != null)) {
                        // don't add this field
                        Log.d("Not adding field to default form = ${it.name}")
                    } else {
                        Log.d("Adding field to default form = ${it.name}")
                        fields.add(it)
                    }
                }
            }
            form.fields = fields
        } else {
            // a form was specified
        }
    }

    Log.d("Form list extraction :")
    formList.forEach { form ->
        form.fields?.forEach {
            Log.d("XX: DM [${form.dataModel.name}], fieldName: ${it.name}, path: ${it.path}, field: $it")
        }
    }

    return formList
}

fun JSONObject.getSearchFields(dataModelList: List<DataModel>, catalogDef: CatalogDef): HashMap<String, List<String>> {
    val searchFields = HashMap<String, List<String>>()
    this.getSafeObject(PROJECT_KEY)?.getSafeObject("list")?.let { listForms ->
        listForms.keys().forEach eachTableIndex@{ tableIndex ->
            if (tableIndex !is String) return@eachTableIndex
            val tableSearchableFields = mutableListOf<String>()
            val tableName = dataModelList.find { it.id == tableIndex }?.name
            listForms.getSafeObject(tableIndex)?.let { listForm ->
                // could be an array or an object (object if only one item dropped)
                val searchableFieldAsArray = listForm.getSafeArray("searchableField")
                if (searchableFieldAsArray != null) {
                    for (ind in 0 until searchableFieldAsArray.length()) {
                        searchableFieldAsArray.getSafeObject(ind)?.let { jsonObject ->
                            jsonObject.getSafeString("kind")?.let { kind ->
                                if (kind == "alias") {
                                    jsonObject.getSafeString("path")?.let { path ->
                                        val unAliasedPath = unAliasPath(path, tableName ?: "", catalogDef)
                                        Log.v("Adding searchField alias $unAliasedPath for table $tableName")
                                        tableSearchableFields.add(unAliasedPath)
                                    }
                                } else {
                                    jsonObject.getSafeString("name")?.let { fieldName ->
                                        Log.v("Adding searchField $fieldName for table $tableName")
                                        tableSearchableFields.add(fieldName)
                                    }
                                }
                            }

                        }
                    }
                } else {
                    val searchableFieldAsObject = listForm.getSafeObject("searchableField")
                    if (searchableFieldAsObject != null) {
                        searchableFieldAsObject.getSafeString("kind")?.let { kind ->
                            if (kind == "alias") {
                                searchableFieldAsObject.getSafeString("path")?.let { path ->
                                    val unAliasedPath = unAliasPath(path, tableName ?: "", catalogDef)
                                    Log.v("Adding searchField alias $unAliasedPath for table $tableName")
                                    tableSearchableFields.add(unAliasedPath)
                                }
                            } else {
                                searchableFieldAsObject.getSafeString("name")?.let { fieldName ->
                                    Log.v("Adding searchField $fieldName for table $tableName")
                                    tableSearchableFields.add(fieldName)
                                }
                            }
                        }
                    } else {
                        Log.w("searchField definition error for table $tableName")
                    }
                }
            }
            if (tableSearchableFields.size != 0) { // if has search field add it
                if (tableName != null) {
                    searchFields[tableName.tableNameAdjustment()] = tableSearchableFields
                } else {
                    Log.e("Cannot get tableName for index $tableIndex when filling search fields")
                }
            }
        }
    } // else no list form, so no search fields
    return searchFields
}

fun JSONObject.getDefaultSortFields(dataModelList: List<DataModel>): MutableList<QueryField> {
    val list = this.getSafeObject(PROJECT_KEY)?.getSafeObject("list")
    val defaultSortFields = mutableListOf<QueryField>()
    list?.names()?.forEach { dataModelId ->
        val tableName = dataModelList.find { it.id == dataModelId }?.name
        if (tableName != null) {

            val item = list.getSafeObject(dataModelId as String) as JSONObject

            var fieldNumber: Int? = null
            var fieldType: String? = null
            var fieldName: String? = null

            val onlySearchableField = item.getSafeObject("searchableField")

            // if only one searchableField it will be a jsonObject and not an array in project.4DMobileApp
            if (onlySearchableField != null) {
                fieldNumber = onlySearchableField.getSafeInt("fieldNumber")
                onlySearchableField.getSafeString("name")?.let { name ->
                    fieldName = name
                }
            } else {
                // if only one searchableField it will be a jsonObject and not an array[] in project.4DMobileApp
                val multipleSearchableFields = item.getSafeArray("searchableField")
                val fields = if (multipleSearchableFields != null && !multipleSearchableFields.isEmpty) {
                    multipleSearchableFields
                } else {
                    item.getSafeArray("fields")
                }

                // skip if photo 'filetype = 3
                val defaultSortField = fields?.filter { it.toString() != "null" }?.firstOrNull {
                    (it as JSONObject).getSafeInt("fieldType") != 3 && it.getSafeString("kind") != "alias"
                }

                if (defaultSortField != null) {
                    (defaultSortField as JSONObject).getSafeString("name")?.let { name ->
                        fieldName = name
                    }
                    fieldNumber = defaultSortField.getSafeInt("fieldNumber")

                } else {
                    // if no search field and no field visible we use the primary key as default sort field
                    fieldName = "__KEY"
                }
            }

            if (fieldNumber != null) {
                dataModelList.find { dataModel ->
                    dataModel.id == dataModelId
                }?.fields?.find { field -> field.id == fieldNumber.toString() }?.let { field ->
                    fieldType = when (field.fieldType) {
                        // date field
                        4 -> {
                            if (!field.format.isNullOrEmpty()) {
                                field.format
                            } else {
                                "mediumDate"  //If format is null or empty return default format
                            }
                        }
                        // time field
                        11 -> {
                            if (!field.format.isNullOrEmpty()) {
                                field.format
                            } else {
                                "mediumTime"  //If format is null or empty return default format
                            }
                        }
                        else -> {
                            field.valueType
                        }
                    }
                }

            }

            fieldName?.let { name ->
                fieldType?.let { type ->
                    defaultSortFields.add(QueryField(name, type, null, null, null, tableName.tableNameAdjustment()))
                }
            }
        }
    }

    return defaultSortFields
}

fun JSONObject.getSectionFields(dataModelList: List<DataModel>): MutableList<QueryField> {

    val list = this.getSafeObject(PROJECT_KEY)?.getSafeObject("list")
    val sectionFields = mutableListOf<QueryField>()
    list?.names()?.forEach { dataModelId ->
        val tableName = dataModelList.find { it.id == dataModelId }?.name
        if (tableName != null) {
            val item = list.getSafeObject(dataModelId as String) as JSONObject
            item.getSafeObject("sectionField")?.let {
                val fieldNumber = it.getSafeInt("fieldNumber")

                val kind = it.getSafeString("kind")
                val path = it.getSafeString("path")
                var tableNumber = dataModelId.toIntOrNull() //the data mode ID is the table number in project.4DMobileApp.json

                it.getSafeString("name")?.let { fieldName ->
                    val fieldType = it.getSafeInt("fieldType")
                    var valueType: String? = null
                    dataModelList.find { dataModel ->
                        dataModel.id == dataModelId
                    }?.fields?.find { field -> field.id == fieldNumber.toString() }?.let { field ->
                        // if the field is time or date we return the format instead (fullDate, shortTime, Duration ...)
                        valueType = when (fieldType) {
                            // date field
                            4 -> {
                                if (!field.format.isNullOrEmpty()) {
                                    field.format
                                } else {
                                    "mediumDate"  //If format is null or empty return default format
                                }
                            }
                            // time field
                            11 -> {
                                if (!field.format.isNullOrEmpty()) {
                                    field.format
                                } else {
                                    "mediumTime"  //If format is null or empty return default format
                                }
                            }

                            else -> {
                                field.valueType
                            }
                        }
                    }

                    // Adjust property name exp : adresse?.Name => adresse?.name , otherwise section will not work
                    val adjustedFieldName = if (fieldName.contains(".")) {
                        val propertyName = fieldName.split(".").last()
                        fieldName.replace(propertyName, propertyName.fieldAdjustment())
                    } else {
                        fieldName
                    }

                    // Adjust relation name exp : Adresse?.Name => adresse.Name

                    val adjustedPath = if (fieldName.contains(".")) {
                        val relationName = fieldName.split(".").first()
                        fieldName.replace(relationName, relationName.fieldAdjustment())
                    } else {
                        fieldName
                    }

                    if (kind == "alias") {
                        path?.let { p ->
                            if ((p.count { value -> value == '.' } <= 1) && (path != fieldName) /* Avoid  crash when the field is a relation to an alias fiels exp: Employe -> Room -> AliasRoomName (roomName field) */) {
                                sectionFields.add(QueryField(adjustedFieldName, valueType
                                        ?: "", kind, path.decapitalize(), tableNumber, tableName.tableNameAdjustment()))
                            }
                        }
                    } else {
                        sectionFields.add(QueryField(adjustedFieldName, valueType
                                ?: "", kind, adjustedPath, tableNumber, tableName.tableNameAdjustment()))
                    }

                }
            }
        }
    }
    return sectionFields
}

fun JSONObject.getDeepLinkScheme(): DeepLink {
    val deeplinkObject = this.getSafeObject(PROJECT_KEY)?.getSafeObject("deepLinking")
    return DeepLink(scheme = deeplinkObject?.getSafeString("urlScheme")?.removeSuffix("://")?.toLowerCase() ?: "",
            enabled = deeplinkObject?.getSafeBoolean("enabled") ?: false)

}

fun JSONObject.getUniversalLink(bundleIdentifier: String? = null): UniversalLink {
    val deeplinkObject = this.getSafeObject(PROJECT_KEY)?.getSafeObject("deepLinking")

    val associatedDomain = deeplinkObject?.getSafeString("associatedDomain")
    val host = associatedDomain?.substringAfter("://", associatedDomain)?.substringBefore("/") ?: ""
    val scheme = associatedDomain?.substringBefore("://", "https") ?: ""
    val pathPrefix = associatedDomain?.substringAfter(host, "") ?: ""
    val pathPattern = if (bundleIdentifier.isNullOrEmpty()) {
        "/mobileapp/\$/show"
    } else {
        "$pathPrefix/.*$bundleIdentifier/show"
    }    // ex: /.*com.test.Tasks/show
    val enabled = deeplinkObject?.getSafeBoolean("enabled") ?: false

    return UniversalLink(host = host, scheme = scheme, pathPattern = pathPattern, enabled = enabled)
}


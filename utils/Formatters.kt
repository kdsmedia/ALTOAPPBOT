import org.json.JSONObject

fun getFieldMappingFormatter(manifestContent: JSONObject, format: String): FieldMappingFormatter =
    FieldMappingFormatter(
        binding = manifestContent.getSafeString("binding"),
        choiceList = getChoiceList(manifestContent),
        type = manifestContent.getSafeString("type") ?: manifestContent.getSafeArray("type")
            .getStringList(), // type can be a String or a JSONArray
        name = format,
        imageWidth = getSize(manifestContent, "width"),
        imageHeight = getSize(manifestContent, "height"),
        tintable = manifestContent.getSafeObject("assets")?.getSafeBoolean("tintable"),
        target = manifestContent.getSafeString("target") ?: manifestContent.getSafeArray("target")
            .getStringList(), // target can be a String or a JSONArray,
        capabilities = manifestContent.getSafeObject("capabilities")?.checkCapabilities()
    )

fun getFieldMappingKotlinInputControl(manifestContent: JSONObject, format: String): FieldMappingKotlinInputControl =
    FieldMappingKotlinInputControl(
        type = manifestContent.getSafeString("type") ?: manifestContent.getSafeArray("type")
            .getStringList(), // type can be a String or a JSONArray
        name = format,
        target = manifestContent.getSafeString("target") ?: manifestContent.getSafeArray("target")
            .getStringList(), // target can be a String or a JSONArray,
        capabilities = manifestContent.getSafeObject("capabilities")?.checkCapabilities(),
    )

fun getFieldMappingLoginForm(manifestContent: JSONObject, format: String): FieldMappingLoginForm =
    FieldMappingLoginForm(
        type = manifestContent.getSafeString("type") ?: manifestContent.getSafeArray("type")
            .getStringList(), // type can be a String or a JSONArray
        name = format,
        target = manifestContent.getSafeString("target") ?: manifestContent.getSafeArray("target")
            .getStringList(), // target can be a String or a JSONArray,
        capabilities = manifestContent.getSafeObject("capabilities")?.checkCapabilities(),
    )

fun getFieldMappingDefaultInputControl(manifestContent: JSONObject): FieldMappingDefaultInputControl =
    FieldMappingDefaultInputControl(
        binding = manifestContent.getSafeString("binding"),
        choiceList = getChoiceList(manifestContent),
        type = manifestContent.getSafeString("type") ?: manifestContent.getSafeArray("type")
            .getStringList(), // type can be a String or a JSONArray
        name = manifestContent.getSafeString("name"),
        format = manifestContent.getSafeString("format"),
        imageWidth = getSize(manifestContent, "width"),
        imageHeight = getSize(manifestContent, "height")
    )

fun getChoiceList(manifestContent: JSONObject): Any {
    Log.d("getChoiceList, manifestContent = $manifestContent")
    return if (manifestContent.getSafeObject("choiceList")?.getSafeObject("dataSource") != null) {
        mapOf("dataSource" to manifestContent.getSafeObject("choiceList")?.getSafeObject("dataSource")?.getDataSource())
    } else {
        manifestContent.getSafeObject("choiceList")?.toStringMap()
            ?: manifestContent.getSafeArray("choiceList")
                .getStringList()  // choiceList can be a JSONObject or a JSONArray
    }
}

private fun JSONObject.getDataSource(): Any {
    val dataSource = InputControlDataSource()
    getSafeString("dataClass")?.let { dataSource.dataClass = it }
    getSafeString("field")?.let { dataSource.field = it }
    getSafeString("entityFormat")?.let { dataSource.entityFormat = it }
    getSafeBoolean("currentEntity")?.let { dataSource.currentEntity = it }

    getSafeBoolean("search")?.let { dataSource.search = it }
    getSafeString("search")?.let { dataSource.search = it }
    getSafeArray("search")?.let { dataSource.search = it.getStringList() }

    getSafeString("order")?.let { dataSource.order = it }

    getSafeString("sort")?.let { dataSource.sort = it }
    getSafeArray("sort")?.let { sortArray ->
        if (sortArray.getSafeString(0) != null) { // list of String
            val list = mutableListOf<Any>()
            for (i in 0 until sortArray.length()) {
                sortArray.getSafeString(i)?.let { list.add(it) }
            }
            dataSource.sort = list
        } else { // list of objects
            val list = mutableListOf<Any>()
            for (i in 0 until sortArray.length()) {
                sortArray.getSafeObject(i)?.toStringMap()?.let { map ->
                    list.add(map)
                }
            }
            dataSource.sort = list
        }
    }
    getSafeObject("sort")?.let { dataSource.sort = it.toStringMap() }
    return dataSource
}

fun getSize(manifestContent: JSONObject, type: String): Int? =
    manifestContent.getSafeObject("assets")?.getSafeObject("size")?.getSafeInt(type)
        ?: manifestContent.getSafeObject("assets")?.getSafeInt("size")

fun FieldMappingFormatter.isValidFormatter(): Boolean =
    (this.binding == "localizedText" || this.binding == "imageNamed")
            && this.choiceList != null && this.name != null

fun FieldMappingFormatter.isValidKotlinCustomDataFormatter(): Boolean {
    return this.name != null && !this.binding.isNullOrEmpty() && isTargetOk(target)
}

fun FieldMappingKotlinInputControl.isValidKotlinInputControl(): Boolean {
    return this.name != null && isTargetOk(target)
}

fun FieldMappingLoginForm.isValidLoginForm(): Boolean {
    return this.name != null && isTargetOk(target)
}

private fun isTargetOk(target: Any?): Boolean {
    return when (target) {
        null -> true
        is String -> target == "android"
        is List<*> -> target.contains("android") || target.isEmpty()
        else -> false
    }
}

fun FieldMappingDefaultInputControl.isValidDefaultInputControl(): Boolean {
    Log.d("isValidDefaultInputControl, this = $this")
    return this.name != null && "/" + this.format in InputControl.defaultInputControls && this.choiceList != null
}

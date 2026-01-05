data class TemplateTableFiller(
    val name: String,
    val name_original: String,
    val nameCamelCase: String,
    val concat_fields: String,
    val label: String,
    val type: String // 'type' is here for Object type which has 'name' Map and 'type' Map<String, Any>
)

fun getTemplateTableFiller(name: String): TemplateTableFiller =
    TemplateTableFiller(
        name = name,
        name_original = name,
        nameCamelCase = name.toLowerCase(),
        concat_fields = "",
        label = name,
        type = getTemplateTableFillerType(name)
    )

fun getTemplateTableFillerType(name: String): String =
    when (name) {
        "Map" -> "Map<String, Any>"
        else -> name
    }

fun DataModel.getTemplateTableFiller(): TemplateTableFiller =
    TemplateTableFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name,
        nameCamelCase = this.name.dataBindingAdjustment(),
        concat_fields = this.fields?.joinToString { "\"${it.name}\"" } ?: "",
        label = this.label ?: "",
        type = this.name.tableNameAdjustment())
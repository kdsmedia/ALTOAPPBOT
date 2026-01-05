data class TemplateFieldFiller(
    val name: String,
    val fieldTypeString: String,
    val variableType: String,
    val name_original: String
)

fun Field.getTemplateFieldFiller(fieldTypeString: String): TemplateFieldFiller =
    TemplateFieldFiller(
        name = this.name.fieldAdjustment(),
        fieldTypeString = fieldTypeString.tableNameAdjustment(),
        variableType = this.variableType,
        name_original = this.name
    )
data class TemplateFormatterFiller(
    val formatterName: String,
    val imageName: String,
    val resourceName: String,
    val resourceNameDarkMode: String,
    val darkModeExists: Boolean
)

fun getTemplateFormatterFiller(
    formatterName: String,
    imageName: String,
    pair: Pair<String, String>
): TemplateFormatterFiller =
    TemplateFormatterFiller(
        formatterName = formatterName,
        imageName = imageName,
        resourceName = pair.first,
        resourceNameDarkMode = pair.second,
        darkModeExists = pair.second.isNotEmpty()
    )
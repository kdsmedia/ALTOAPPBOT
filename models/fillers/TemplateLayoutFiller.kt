
data class TemplateLayoutFiller(
    val name: String,
    val name_original: String,
    val nameLowerCase: String,
    val nameCamelCase: String,
    val label: String,
    val hasIcon: Boolean,
    val icon: String,
    val isGlobalAction: Boolean
)

fun DataModel.getTemplateLayoutFillerForNavigation(): TemplateLayoutFiller =
    TemplateLayoutFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name.encode(),
        nameLowerCase = this.name.tableNameAdjustment().toLowerCase(),
        nameCamelCase = this.name.dataBindingAdjustment(),
        label = this.getLabel().encode(),
        hasIcon = !this.iconPath.isNullOrEmpty(),
        icon = this.iconPath ?: "",
        isGlobalAction = false
    )

fun Action.getTemplateLayoutFillerForNavigation(): TemplateLayoutFiller =
    TemplateLayoutFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name.encode(),
        nameLowerCase = "$ACTION_FROM_NAV_BAR_KEY${this.name.tableNameAdjustment().toLowerCase()}",
        nameCamelCase = this.name.dataBindingAdjustment(),
        label = this.getLabel().encode(),
        hasIcon = !this.icon.isNullOrEmpty(),
        icon = correctIconPath(this.icon ?: ""),
        isGlobalAction = true
    )

fun DataModel.getTemplateLayoutFiller(): TemplateLayoutFiller =
    TemplateLayoutFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name,
        nameLowerCase = this.name.toLowerCase().fieldAdjustment(),
        nameCamelCase = this.name.dataBindingAdjustment(),
        label = this.getLabel(),
        hasIcon = !this.iconPath.isNullOrEmpty(),
        icon = this.iconPath ?: "",
        isGlobalAction = false
    )
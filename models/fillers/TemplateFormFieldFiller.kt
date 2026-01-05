data class TemplateFormFieldFiller(
    val name: String,
    val label: String,
    val shortLabel: String,
    val viewId: Int,
    val isRelation: Boolean,
    val isImage: Boolean,
    val imageSourceTableName: String,
    val accessor: String,
    val isCustomFormat: Boolean,
    val formatFieldName: String,
    val isImageNamed: Boolean,
    val formatType: String,
    val imageFieldName: String,
    val keyAccessor: String,
    val fieldTableName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val hasIcon: Boolean,
    val iconPath: String,
    val labelHasPercentPlaceholder: Boolean,
    val labelWithPercentPlaceholder: String,
    val shortLabelHasPercentPlaceholder: Boolean,
    val shortLabelWithPercentPlaceholder: String,
    val entryRelation: String,
    val altButtonText: String,
    val isKotlinCustomFormat: Boolean,
    val kotlinCustomFormatBinding: String
)

fun Field.getTemplateFormFieldFiller(
    i: Int,
    dataModelList: List<DataModel>,
    form: Form,
    formatType: String,
    isImageNamed: Boolean,
    imageWidth: Int,
    imageHeight: Int,
    wholeFormHasIcons: Boolean,
    pathHelper: PathHelper,
    catalogDef: CatalogDef
): TemplateFormFieldFiller {
    Log.d("createDetailFormField : field = $this")

    val templateFormFieldFiller = TemplateFormFieldFiller(
        name = this.getFieldAliasName(dataModelList),
        label = getLabelWithFixes(dataModelList, form, this),
        shortLabel = getShortLabelWithFixes(dataModelList, form, this),
        viewId = i,
        isRelation = isRelationWithFixes(dataModelList, form, this),
        isImage = this.isImage(),
        imageSourceTableName = destBeforeField(catalogDef, form.dataModel.name, this.path),
        accessor = this.getLayoutVariableAccessor(),
        isCustomFormat = pathHelper.isValidFormatter(formatType),
        formatFieldName = this.name,
        isImageNamed = isImageNamed,
        formatType = formatType,
        imageFieldName = this.getImageFieldName(),
        keyAccessor = this.getFieldKeyAccessor(dataModelList),
        fieldTableName = form.dataModel.name,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        hasIcon = wholeFormHasIcons,
        iconPath = getIcon(dataModelList, form, this),
        labelHasPercentPlaceholder = hasLabelPercentPlaceholder(dataModelList, form.dataModel, this),
        labelWithPercentPlaceholder = getLabelWithPercentPlaceholder(dataModelList, form, this, catalogDef),
        shortLabelHasPercentPlaceholder = hasShortLabelPercentPlaceholder(dataModelList, form.dataModel, this),
        shortLabelWithPercentPlaceholder = getShortLabelWithPercentPlaceholder(dataModelList, form, this, catalogDef),
        entryRelation = getEntryRelation(dataModelList, form.dataModel.name, this, catalogDef),
        altButtonText = if (hasFieldPlaceholder(getShortLabelWithFixes(dataModelList, form, this), dataModelList, form.dataModel, this)) "" else getShortLabelWithFixes(dataModelList, form, this),
        isKotlinCustomFormat = !pathHelper.isValidFormatter(formatType) && pathHelper.isValidKotlinCustomFormatter(formatType),
        kotlinCustomFormatBinding = if (!pathHelper.isValidFormatter(formatType) && pathHelper.isValidKotlinCustomFormatter(formatType)) pathHelper.getKotlinCustomFormatterBinding(formatType) else ""
    )
    Log.d("createDetailFormField : templateFormFieldFiller = $templateFormFieldFiller")
    return templateFormFieldFiller
}
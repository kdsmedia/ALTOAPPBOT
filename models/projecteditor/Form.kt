data class Form(
    var dataModel: DataModel,
    var name: String? = null,
    var fields: List<Field>? = null,
    var searchableWithBarcode: Boolean = false
)
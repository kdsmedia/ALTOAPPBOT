data class DataModel(
    var id: String,
    var name: String,
    var label: String? = null,
    var shortLabel: String? = null,
    var fields: MutableList<Field>? = null,
    var query: String? = null,
    var iconPath: String? = null,
    var isSlave: Boolean? = null,
    var relations: MutableList<Relation>? = null
)

fun DataModel.getLabel(): String {
    shortLabel?.let { if (it.isNotEmpty()) return it }
    label?.let { if (it.isNotEmpty()) return it }
    return this.name
}
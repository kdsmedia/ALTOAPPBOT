data class Actions(
    val table: Map<String, List<Action>>,
    val currentRecord: Map<String, List<Action>>,
    val global: List<Action>
)

data class Action(
    val name: String,
    var shortLabel: String? = null,
    var label: String? = null,
    var scope: String? = null,
    var tableNumber: Int? = null,
    var icon: String? = null,
    var preset: String? = null,
    var style: String? = null,
    var parameters: List<ActionParameter>? = null,
    var description: String? = null
)

data class ActionParameter(
    val name: String,
    var label: String? = null,
    var shortLabel: String? = null,
    var type: String? = null,
    var default: String? = null,
    var placeholder: String? = null,
    var format: String? = null,
    var source: String? = null,
    var fieldNumber: Int? = null,
    var fieldName: String? = null,
    var tableName: String? = null,
    var defaultField: String? = null,
    var rules: List<Any>? = null
)

fun Action.getLabel(): String {
    shortLabel?.let { if (it.isNotEmpty()) return it }
    label?.let { if (it.isNotEmpty()) return it }
    return this.name
}

object InputControl {

    val defaultInputControls = listOf("/push", "/segmented", "/popover", "/sheet", "/picker")
}
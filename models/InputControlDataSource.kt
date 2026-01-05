data class InputControlDataSource(
    var dataClass: String? = null,
    var field: String? = null,
    var entityFormat: String? = null,
    var search: Any? = null, // Bool, String, Array
    var order: String? = null,
    var sort: Any? = null, // String, Array (of String, Object (field, order) , Object (field, order)
    var currentEntity: Boolean? = null
)

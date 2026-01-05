data class TableInfo(
    val originalName: String,
    val label: String,
    val query: String,
    val fields: LinkedHashMap<String, String>,
    val searchFields: String,
    val searchableWithBarcode: Boolean
)
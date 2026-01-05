data class QueryField(var name: String, val valueType: String, val kind: String?, val path: String?, val tableNumber: Int?, val tableName: String) {
    fun isAlias(): Boolean {
        return kind == "alias"
    }
}

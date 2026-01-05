data class FieldMappingFormatter(
    val binding: String?,
    val choiceList: Any?,  // choiceList can be a JSONObject or a JSONArray
    val type: Any?,  // type can be a String or a JSONArray
    val name: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val tintable: Boolean?,
    val target: Any?,
    val capabilities : List<String>?
) {
    fun isImageNamed() = this.binding == "imageNamed"
}

data class FieldMappingKotlinInputControl(
    val type: Any?,  // type can be a String or a JSONArray
    val name: String?,
    val target: Any?,
    val capabilities : List<String>?
)

data class FieldMappingDefaultInputControl(
    val binding: String?,
    val type: Any?,  // type can be a String or a JSONArray
    val choiceList: Any?,  // choiceList can be a JSONObject or a JSONArray
    val name: String?,
    var format: String?,
    val imageWidth: Int?,
    val imageHeight: Int?
)

data class FieldMappingLoginForm(
    val type: Any?,  // type can be a String or a JSONArray
    val name: String?,
    val target: Any?,
    val capabilities : List<String>?
)

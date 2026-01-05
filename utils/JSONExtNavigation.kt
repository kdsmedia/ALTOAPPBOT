import org.json.JSONObject

fun JSONObject.getNavigationTableList(): List<String> {
    Log.d("this.getSafeObject(PROJECT_KEY)?.getSafeObject(MAIN_KEY) = ${this.getSafeObject(PROJECT_KEY)?.getSafeObject(MAIN_KEY)}")
    val array = this.getSafeObject(PROJECT_KEY)?.getSafeObject(MAIN_KEY)?.getSafeArray(ORDER_KEY)
    val list = mutableListOf<String>()
    array?.let {
        for (i in 0 until array.length()) {
            val key = array.getSafeObject(i)?.getSafeString("action") ?: array.getSafeString(i).toString()
            list.add(key)
        }
    }
    return list
}

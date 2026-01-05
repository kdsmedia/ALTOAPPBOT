data class TemplateLayoutTypeFiller(
    val name: String,
    val layout_manager_type: String,
    val isSwipeAllowed: Boolean,
    val isGoogleMapsPlatformUsed: Boolean
)

fun getTemplateLayoutTypeFiller(tableName: String, formPath: String): TemplateLayoutTypeFiller =
    TemplateLayoutTypeFiller(
        name = tableName,
        layout_manager_type = getLayoutManagerType(formPath),
        isSwipeAllowed = isSwipeAllowed(formPath),
        isGoogleMapsPlatformUsed = isGoogleMapsPlatformUsed(formPath)
    )

fun getLayoutManagerType(formPath: String): String {
    Log.i("getLayoutManagerType: $formPath")
    var type = "Collection"
    getManifestJSONContent(formPath)?.let {
        type = it.getSafeObject("tags")?.getSafeString("___LISTFORMTYPE___") ?: "Collection"
    }
    return when (type) {
        "Collection" -> "LayoutType.GRID"
        "Table" -> "LayoutType.LINEAR"
        else -> "LayoutType.LINEAR"
    }
}

fun isSwipeAllowed(formPath: String): Boolean {
    getManifestJSONContent(formPath)?.let {
        val isSwipeAllowed: Boolean? = it.getSafeObject("tags")?.getSafeBoolean("swipe")
        isSwipeAllowed?.let { isAllowed ->
            return isAllowed
        }
    }
    return when (getLayoutManagerType(formPath)) {
        "LayoutType.GRID" -> false
        "LayoutType.LINEAR" -> true
        else -> true
    }
}

fun isGoogleMapsPlatformUsed(formPath: String): Boolean {
    getManifestJSONContent(formPath)?.let {
        val isGMPUsed: Boolean? = it.getSafeObject("tags")?.getSafeBoolean("google_maps_platform")
        isGMPUsed?.let { isUsed ->
            return isUsed
        }
    }
    return false
}

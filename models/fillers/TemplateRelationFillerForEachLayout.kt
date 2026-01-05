import java.util.*

data class TemplateRelationFillerForEachLayout(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val tableNameLowercase: String,
    val associatedViewId: Int,
    val isSubRelation: Boolean,
    val subRelation_inverse_name: String,
    val navbarTitle: String,
    val relation_source_camelCase: String,
    val relation_target_camelCase: String,
    val isAlias: Boolean,
    val path: String,
    val pathToOneWithoutFirst: String,
    val pathToManyWithoutFirst: String
)

fun getTemplateRelationFillerForLayout(
    source: String,
    viewId: Int,
    navbarTitle: String,
    relation: Relation,
    catalogDef: CatalogDef
): TemplateRelationFillerForEachLayout =
    TemplateRelationFillerForEachLayout(
        relation_source = source.tableNameAdjustment(),
        relation_target = relation.target.tableNameAdjustment(),
        relation_name = relation.name.relationNameAdjustment(),
        tableNameLowercase = source.dataBindingAdjustment().decapitalize(Locale.getDefault()),
        associatedViewId = viewId,
        isSubRelation = relation.name.fieldAdjustment().contains("."),
        subRelation_inverse_name = getSubRelationInverseName(relation.name),
        navbarTitle = navbarTitle,
        relation_source_camelCase = source.dataBindingAdjustment(),
        relation_target_camelCase = relation.target.dataBindingAdjustment(),
        isAlias = relation.path.contains("."),
        path = relation.path.relationPathAdjustment().ifEmpty { relation.name.fieldAdjustment() },
        pathToOneWithoutFirst = getPathToOneWithoutFirst(relation, catalogDef),
        pathToManyWithoutFirst = getPathToManyWithoutFirst(relation, catalogDef)
    )


fun getPathToOneWithoutFirst(aliasRelation: Relation, catalogDef: CatalogDef): String {
    Log.d("getPathToOneWithoutFirst, aliasRelation = $aliasRelation")
    var path = ""
    val pathList = aliasRelation.path.split(".")
    var nextSource = aliasRelation.source
    var tmpNextPath = aliasRelation.path
    pathList.forEachIndexed { index, pathPart ->
        Log.d("getPathToOneWithoutFirst, pathPart = $pathPart")
        catalogDef.relations.find { it.source == nextSource && it.name == pathPart }?.let { relation ->
            if (index > 0) {
                if (path.isNotEmpty())
                    path += "?."

                Log.d("tmpNextPath = $tmpNextPath")
                path += tmpNextPath.relationNameAdjustment()
            }
            nextSource = relation.target
            tmpNextPath = tmpNextPath.substringAfter(".")
        }
        Log.d("path building : $path")
    }
    Log.d("final path = $path")
    return path
}

fun getPathToManyWithoutFirst(aliasRelation: Relation, catalogDef: CatalogDef): String {
    Log.d("getPathToManyWithoutFirst, aliasRelation = $aliasRelation")
    var path = ""
    val pathList = aliasRelation.path.split(".")
    var nextSource = aliasRelation.source
    var previousRelationType = RelationType.MANY_TO_ONE
    var tmpNextPath = aliasRelation.path
    pathList.forEachIndexed { index, pathPart ->
        Log.d("getPathToManyWithoutFirst, pathPart = $pathPart")
        catalogDef.relations.find { it.source == nextSource && it.name == pathPart }?.let { relation ->
            if (index > 0) {
                if (path.isNotEmpty())
                    path += "?."

                Log.d("tmpNextPath = $tmpNextPath")

                path += if (previousRelationType == RelationType.ONE_TO_MANY) {
                    "mapNotNull { it.${tmpNextPath.relationNameAdjustment()}"
                } else {
                    tmpNextPath.relationNameAdjustment()
                }
            }
            previousRelationType = relation.type
            nextSource = relation.target
            tmpNextPath = tmpNextPath.substringAfter(".")
        }
        Log.d("path building : $path")
    }
    // remove suffix in case it ends by a 1-N relation
    path = path.removeSuffix("?.mapNotNull { it.")
    repeat(path.count { it == '{' }) {
        path += " }?.takeIf { it.isNotEmpty() }"
    }
    Log.d("final path = $path")
    return path
}

fun getSubRelationInverseName(relationName: String): String = if (relationName.fieldAdjustment().contains("."))
    relationName.fieldAdjustment().split(".").getOrNull(0) ?: ""
else
    ""

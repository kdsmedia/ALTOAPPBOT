data class TemplateRelationFiller(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val relation_name_cap: String = relation_name.capitalize(),
    val inverse_name_cap: String = inverse_name.capitalize(),
    val isSubRelation: Boolean,
    val originalSubRelationName: String,
    val relation_source_camelCase: String,
    val relation_target_camelCase: String,
    val relation_name_original: String,
    val isAlias: Boolean,
    val path: String,
    val relation_embedded_return_type: String,
    val key_name: String,
    val firstIsToMany: Boolean,
    val firstTarget: String,
    val pathToOneWithoutFirst: String,
    val pathToManyWithoutFirst: String
)

fun Relation.getTemplateRelationFiller(catalogDef: CatalogDef): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = this.source.tableNameAdjustment(),
        relation_target = this.target.tableNameAdjustment(),
        relation_name = this.name.relationNameAdjustment(),
        inverse_name = this.inverseName.fieldAdjustment(),
        isSubRelation = false,
        originalSubRelationName = "",
        relation_source_camelCase = this.source.dataBindingAdjustment(),
        relation_target_camelCase = this.target.dataBindingAdjustment(),
        relation_name_original = this.name,
        isAlias = this.path.isNotEmpty(),
        path = this.path,
        relation_embedded_return_type = getEmbeddedReturnType(this),
        key_name = getKeyName(catalogDef,this),
        firstIsToMany = getFirstIsToMany(catalogDef, this),
        firstTarget = getFirstTarget(catalogDef, this),
        pathToOneWithoutFirst = getPathToOneWithoutFirst(this, catalogDef),
        pathToManyWithoutFirst = getPathToManyWithoutFirst(this, catalogDef)
    )

fun getFirstIsToMany(catalogDef: CatalogDef, relation: Relation): Boolean {
    if (relation.path.isEmpty()) {
        return relation.type == RelationType.ONE_TO_MANY
    } else {
        val firstRelationName = relation.path.split(".")[0]
        catalogDef.relations.find { it.source == relation.source && it.name == firstRelationName }?.let { firstRelation ->
            return firstRelation.type == RelationType.ONE_TO_MANY
        }
    }
    return false
}

fun getFirstTarget(catalogDef: CatalogDef, relation: Relation): String {
    if (relation.path.isEmpty()) {
        return relation.target.tableNameAdjustment()
    } else {
        val firstRelationName = relation.path.split(".")[0]
        catalogDef.relations.find { it.source == relation.source && it.name == firstRelationName }?.let { firstRelation ->
            return firstRelation.target.tableNameAdjustment()
        }
    }
    return ""
}

fun getEmbeddedReturnType(relation: Relation): String {
    return if (relation.path.contains(".")) relation.relation_embedded_return_type.tableNameAdjustment() else relation.target.tableNameAdjustment()
}

fun getKeyName(catalogDef: CatalogDef, relation: Relation): String {
    Log.d("getKeyName, relation: $relation")
    var key = ""

    if (relation.path.isEmpty()) {
        key = if (relation.type == RelationType.ONE_TO_MANY)
            relation.inverseName
        else
            relation.name
    } else {
        if (relation.path.count { it == '.' } > 0) {
            Log.d("relation.path contains at least one '.'")
            val firstRelationName = relation.path.split(".")[0]
            // la relation du premier element avant "."
            catalogDef.relations.find { it.source == relation.source && it.name == firstRelationName }?.let { firstRelation ->

                key = if (firstRelation.type == RelationType.ONE_TO_MANY)
                    firstRelation.inverseName
                else
                    firstRelation.name
                Log.d("key = $key")
            }
        } else if (relation.path.count { it == '.' } == 0){ // serv
            key = relation.path.fieldAdjustment()
        }
    }
    return key.fieldAdjustment()
}

data class TemplateRelationForRoomFiller(
    val className: String,
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val relation_name_cap: String = relation_name.capitalize(),
    val relation_source_camelCase: String,
    val key_name: String,
    val relation_embedded_return_type: String,
    val firstIsToMany: Boolean,
    val relation_part_name: String,
    val firstTarget: String
)

fun Relation.getTemplateRelationForRoomFiller(catalogDef: CatalogDef): TemplateRelationForRoomFiller? {
    Log.d("getTemplateRelationForRoomFiller")
    Log.d("relation: $this")
    if (this.path.isEmpty()) {
        val key = if (this.type == RelationType.ONE_TO_MANY)
            this.inverseName
        else
            this.name
        return TemplateRelationForRoomFiller(
            className = getEmbeddedReturnTypeName(source, name),
            relation_source = source.tableNameAdjustment(),
            relation_target = target.tableNameAdjustment(),
            relation_name = name,
            relation_source_camelCase = source.fieldAdjustment(),
            key_name = key.fieldAdjustment(),
            relation_embedded_return_type = target.tableNameAdjustment(),
            firstIsToMany = this.type == RelationType.ONE_TO_MANY,
            relation_part_name = name.fieldAdjustment(),
            firstTarget = target.tableNameAdjustment()
        )
    } else {
        if (this.path.count { it == '.' } > 0) {
            Log.d("path contains at least one '.'")
            val firstRelationName = this.path.split(".")[0]
            Log.d("firstRelationName = $firstRelationName")

            // la relation du premier element avant "."
            catalogDef.relations.find { it.source == this.source && it.name == firstRelationName }?.let { firstRelation ->
//                Log.d("firstRelation: $firstRelation")
                val key = if (firstRelation.type == RelationType.ONE_TO_MANY)
                    firstRelation.inverseName
                else
                    firstRelation.name

                Log.d("key = $key")
                return TemplateRelationForRoomFiller(
                    className = getEmbeddedReturnTypeName(source, name),
                    relation_source = source.tableNameAdjustment(),
                    relation_target = target.tableNameAdjustment(),
                    relation_name = name,
                    relation_source_camelCase = source.fieldAdjustment(),
                    key_name = key.fieldAdjustment(),
                    relation_embedded_return_type = getEmbeddedReturnTypeName(firstRelation.target, this.path.substringAfter(".")),
                    firstIsToMany = firstRelation.type == RelationType.ONE_TO_MANY,
                    relation_part_name = if (this.path.contains(".")) this.path.relationNameAdjustment() else this.path.fieldAdjustment(),
                    firstTarget = getFirstTarget(catalogDef, this)
                )
            }
        }
    }
    return null
}
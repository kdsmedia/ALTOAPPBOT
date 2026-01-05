data class TemplateRelationDefFillerDeepLink(
        val relation_name: String,
        val relation_source_camelCase: String,
        val isAlias: Boolean,
        val relation_source: String,
        val inverse_name: String,
        val pathToManyWithoutFirst: String,
        val navbarTitle: String,
        val path: String,
        val relation_target: String,
        val pathToOneWithoutFirst: String
)

fun TemplateRelationFiller.getTemplateRelationDefFillerDeepLink(relationType: RelationType, navbarTitle: String?): TemplateRelationDefFillerDeepLink =
        TemplateRelationDefFillerDeepLink(
                relation_source = if (relationType == RelationType.ONE_TO_MANY && this.isSubRelation) this.relation_source else this.relation_source,
                relation_name = if (relationType == RelationType.ONE_TO_MANY && this.isSubRelation) this.originalSubRelationName else this.relation_name,
                inverse_name = this.inverse_name,
                path = path.relationPathAdjustment().ifEmpty { this.relation_name.fieldAdjustment() },
                isAlias = this.path.contains("."),
                navbarTitle = navbarTitle ?: this.relation_embedded_return_type,
                pathToManyWithoutFirst = this.pathToManyWithoutFirst,
                relation_source_camelCase = this.relation_source_camelCase,
                relation_target = this.relation_target,
                pathToOneWithoutFirst =this.pathToOneWithoutFirst
        )

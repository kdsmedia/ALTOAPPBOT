data class TemplateRelationDefFiller(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val relationType: String,
    val path: String
)

fun TemplateRelationFiller.getTemplateRelationDefFiller(relationType: RelationType): TemplateRelationDefFiller =
    TemplateRelationDefFiller(
        relation_source = if (relationType == RelationType.ONE_TO_MANY && this.isSubRelation) this.relation_source else this.relation_source,
        relation_target = this.relation_target,
        relation_name = if (relationType == RelationType.ONE_TO_MANY && this.isSubRelation) this.originalSubRelationName else this.relation_name,
        inverse_name = this.inverse_name,
        relationType = if (relationType == RelationType.ONE_TO_MANY) "Relation.Type.ONE_TO_MANY" else "Relation.Type.MANY_TO_ONE",
        path = this.path.relationPathAdjustment()
    )

fun TemplateRelationFiller.getTemplateRelationDefFillerForRelationId(): TemplateRelationDefFiller =
    TemplateRelationDefFiller(
        relation_source = this.relation_source,
        relation_target = "",
        relation_name = this.relation_name,
        inverse_name = "",
        relationType = "",
        path = ""
    )
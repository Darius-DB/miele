package com.miele.ckb.recipe.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Node("Recipe")
@Getter @Setter
public class Recipe {

    @Id
    private String id;
    private String type;
    private Integer schemaVersion;
    private Map<String, String> labels = new HashMap<>();
    @Relationship(type = "HAS_STEP", direction = Relationship.Direction.OUTGOING)
    private List<RecipeStep> steps = new ArrayList<>();

    public Recipe() {
        this.id = UUID.randomUUID().toString();
    }

}
package com.miele.ckb.recipe.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Node("RecipeStep")
@Getter @Setter
public class RecipeStep {

    @Id
    private String id;
    private Integer sequence;
    private Map<String, String> labels = new HashMap<>();

    public RecipeStep() {
        this.id = UUID.randomUUID().toString();
    }


}
package com.miele.ckb.recipe.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Read-side representation of a recipe.
 */
public record RecipeResponse(
        String id,
        String type,
        Integer version,
        Map<String, String> label,
        List<Step> recipeSteps
) {
    public record Step(Integer sequence, Map<String, String> label) { }
}
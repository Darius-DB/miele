package com.miele.ckb.recipe.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter
public class RecipeDTO {


    @NotBlank
    @Pattern(
            regexp = "^https?://.+/type/recipe$",
            message = "type must be a recipe ontology IRI ending with /type/recipe"
    )
    private String type;


    @NotNull
    @JsonProperty("version")
    private Integer version;

    @NotNull
    @Valid
    private LabelDTO label;

    @NotEmpty(message = "a recipe must contain at least one step")
    @Valid
    private List<RecipeStepDTO> recipeSteps;


}
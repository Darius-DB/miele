package com.miele.ckb.recipe.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecipeStepDTO {

    @NotNull
    @Valid
    private LabelDTO label;


    @NotNull
    @Min(1)
    @JsonAlias({"sequence", "order", "position"})
    private Integer sequence;


}
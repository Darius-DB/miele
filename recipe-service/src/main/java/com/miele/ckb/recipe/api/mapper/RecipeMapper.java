package com.miele.ckb.recipe.api.mapper;

import com.miele.ckb.recipe.api.dto.LabelDTO;
import com.miele.ckb.recipe.api.dto.RecipeDTO;
import com.miele.ckb.recipe.api.dto.RecipeStepDTO;
import com.miele.ckb.recipe.domain.Recipe;
import com.miele.ckb.recipe.domain.RecipeStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mapper(componentModel = "spring")
public interface RecipeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schemaVersion", source = "version")
    @Mapping(target = "labels", source = "label", qualifiedByName = "labelToMap")
    @Mapping(target = "steps", source = "recipeSteps")
    Recipe toDomain(RecipeDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "labels", source = "label", qualifiedByName = "labelToMap")
    RecipeStep toDomain(RecipeStepDTO dto);

    List<RecipeStep> toDomainSteps(List<RecipeStepDTO> dtos);

    @Named("labelToMap")
    default Map<String, String> labelToMap(LabelDTO label) {
        if (label == null || label.getTranslations() == null) {
            return new HashMap<>();
        }
        return new HashMap<>(label.getTranslations());
    }
}
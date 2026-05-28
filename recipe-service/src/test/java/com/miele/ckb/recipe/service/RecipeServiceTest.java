package com.miele.ckb.recipe.service;

import com.miele.ckb.recipe.api.dto.LabelDTO;
import com.miele.ckb.recipe.api.dto.RecipeDTO;
import com.miele.ckb.recipe.api.dto.RecipeStepDTO;
import com.miele.ckb.recipe.api.mapper.RecipeMapper;
import com.miele.ckb.recipe.api.mapper.RecipeMapperImpl;
import com.miele.ckb.recipe.domain.Recipe;
import com.miele.ckb.recipe.persistence.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class RecipeServiceTest {

    private RecipeRepository repository;
    private RecipeMapper mapper;
    private RecipeService service;

    @BeforeEach
    void setUp() {
        repository = mock(RecipeRepository.class);
        mapper = new RecipeMapperImpl();          // use the generated impl
        service = new RecipeService(repository, mapper);
        when(repository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ingestsValidRecipe() {
        RecipeDTO dto = sampleDto(1);

        Recipe saved = service.ingest(dto);

        assertThat(saved.getType()).isEqualTo("http://miele.com/service/ckb/1.0/type/recipe");
        assertThat(saved.getSchemaVersion()).isEqualTo(1);
        assertThat(saved.getLabels()).containsEntry("de", "Thorben's Rezept komplett");
        assertThat(saved.getSteps()).hasSize(1);
        verify(repository, times(1)).save(any(Recipe.class));
    }

    @Test
    void rejectsUnsupportedVersion() {
        assertThatThrownBy(() -> service.ingest(sampleDto(2)))
                .isInstanceOf(UnsupportedRecipeVersionException.class);
    }

    private RecipeDTO sampleDto(int version) {
        RecipeDTO dto = new RecipeDTO();
        dto.setType("http://miele.com/service/ckb/1.0/type/recipe");
        dto.setVersion(version);

        LabelDTO recipeLabel = new LabelDTO();
        recipeLabel.add("de", "Thorben's Rezept komplett");
        dto.setLabel(recipeLabel);

        RecipeStepDTO step = new RecipeStepDTO();
        LabelDTO stepLabel = new LabelDTO();
        stepLabel.add("de", "Öl in der Pfanne erhitzen");
        step.setLabel(stepLabel);
        step.setSequence(1);
        dto.setRecipeSteps(List.of(step));
        return dto;
    }
}
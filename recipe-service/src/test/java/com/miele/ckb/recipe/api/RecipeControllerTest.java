package com.miele.ckb.recipe.api;

import com.miele.ckb.recipe.api.dto.RecipeDTO;
import com.miele.ckb.recipe.api.mapper.RecipeMapper;
import com.miele.ckb.recipe.domain.Recipe;
import com.miele.ckb.recipe.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import(RecipeControllerTest.MockedServiceConfig.class)
class RecipeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired RecipeService service;

    @Test
    void returns201ForValidRequest() throws Exception {
        Recipe stored = new Recipe();
        stored.setType("http://miele.com/service/ckb/1.0/type/recipe");
        stored.setSchemaVersion(1);
        when(service.ingest(any(RecipeDTO.class))).thenReturn(stored);

        String json = """
            {
              "type": "http://miele.com/service/ckb/1.0/type/recipe",
              "version": 1,
              "label": { "de": "Thorben's Rezept komplett" },
              "recipeSteps": [
                { "label": { "de": "Öl in der Pfanne erhitzen" }, "sequence": 1 }
              ]
            }
            """;

        mvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("http://miele.com/service/ckb/1.0/type/recipe"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void returns400WhenStepsMissing() throws Exception {
        String json = """
            {
              "type": "http://miele.com/service/ckb/1.0/type/recipe",
              "version": 1,
              "label": { "de": "x" }
            }
            """;

        mvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class MockedServiceConfig {
        @Bean RecipeService recipeService() { return mock(RecipeService.class); }
        @Bean RecipeMapper recipeMapper() { return new com.miele.ckb.recipe.api.mapper.RecipeMapperImpl(); }
    }
}
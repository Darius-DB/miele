package com.miele.ckb.recipe.api;

import com.miele.ckb.recipe.api.dto.RecipeCreatedResponse;
import com.miele.ckb.recipe.api.dto.RecipeDTO;
import com.miele.ckb.recipe.domain.Recipe;
import com.miele.ckb.recipe.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RecipeCreatedResponse> create(@Valid @RequestBody RecipeDTO dto) {
        Recipe saved = service.ingest(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(new RecipeCreatedResponse(
                        saved.getId(), saved.getType(), saved.getSchemaVersion()));
    }
}
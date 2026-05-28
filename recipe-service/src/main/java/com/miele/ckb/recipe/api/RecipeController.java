package com.miele.ckb.recipe.api;

import com.miele.ckb.recipe.api.dto.RecipeCreatedResponse;
import com.miele.ckb.recipe.api.dto.RecipeDTO;
import com.miele.ckb.recipe.api.dto.RecipeResponse;
import com.miele.ckb.recipe.api.mapper.RecipeMapper;
import com.miele.ckb.recipe.domain.Recipe;
import com.miele.ckb.recipe.service.RecipeNotFoundException;
import com.miele.ckb.recipe.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService service;
    private final RecipeMapper mapper;

    public RecipeController(RecipeService service, RecipeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
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

    @GetMapping("/{id}")
    public RecipeResponse getById(@PathVariable String id) {
        return service.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @GetMapping
    public List<RecipeResponse> list() {
        return service.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }
}
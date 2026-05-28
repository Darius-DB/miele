package com.miele.ckb.recipe.service;

import com.miele.ckb.recipe.api.dto.RecipeDTO;
import com.miele.ckb.recipe.api.mapper.RecipeMapper;
import com.miele.ckb.recipe.domain.Recipe;
import com.miele.ckb.recipe.persistence.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    /** Highest payload version this service understands today (Q3). */
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final RecipeRepository repository;
    private final RecipeMapper mapper;

    public RecipeService(RecipeRepository repository, RecipeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public Recipe ingest(RecipeDTO dto) {
        rejectUnsupportedVersion(dto);

        Recipe domain = mapper.toDomain(dto);

        Recipe saved = repository.save(domain);
        log.info("Persisted recipe id={} schemaVersion={}",
                saved.getId(), saved.getSchemaVersion());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Recipe> findById(String id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Recipe> findAll() {
        return repository.findAll();
    }


    private void rejectUnsupportedVersion(RecipeDTO dto) {
        if (dto.getVersion() == null || dto.getVersion() > SUPPORTED_SCHEMA_VERSION) {
            throw new UnsupportedRecipeVersionException(dto.getVersion());
        }
    }
}
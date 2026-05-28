package com.miele.ckb.recipe.persistence;

import com.miele.ckb.recipe.domain.Recipe;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the domain port {@link RecipeRepository}
 * on top of the Spring Data Neo4j repository.
 */
@Repository
public class RecipeRepositoryAdapter implements RecipeRepository {

    private final Neo4jRecipeRepository delegate;

    public RecipeRepositoryAdapter(Neo4jRecipeRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Recipe save(Recipe recipe) {
        return delegate.save(recipe);
    }

    @Override
    public Optional<Recipe> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public List<Recipe> findAll() {
        return (List<Recipe>) delegate.findAll();
    }
}
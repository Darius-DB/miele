package com.miele.ckb.recipe.persistence;

import com.miele.ckb.recipe.domain.Recipe;

import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence port for {@link Recipe} aggregates.
 *
 * The service layer depends ONLY on this interface, never on
 * a concrete graph-database client. Swapping Neo4j for another
 * triple-store / graph store would only require a new adapter
 * implementing this contract -- no service-layer changes.
 *
 * This is the concrete realization of the OO-encapsulation
 * advantage discussed in Q2.
 */
public interface RecipeRepository {

    /**
     * Persists a fully populated recipe aggregate (recipe + steps).
     *
     * @return the stored aggregate, including any ids assigned
     *         during persistence.
     */
    Recipe save(Recipe recipe);

    Optional<Recipe> findById(String id);

    List<Recipe> findAll();
}
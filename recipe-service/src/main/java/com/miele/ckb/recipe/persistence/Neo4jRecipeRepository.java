package com.miele.ckb.recipe.persistence;

import com.miele.ckb.recipe.domain.Recipe;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * Spring Data Neo4j repository for {@link Recipe}.
 *
 * Inherited methods ({@code save}, {@code findById}, ...) are
 * implemented automatically by Spring Data based on the entity
 * mapping. No hand-written Cypher is required for basic CRUD,
 * which is exactly the kind of duplication that native queries
 * would cause across the code base (Q2).
 */
public interface Neo4jRecipeRepository extends Neo4jRepository<Recipe, String> {
    // Custom Cypher (if ever needed) is added here -- and ONLY here.
}
package com.miele.ckb.recipe.service;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(String id) {
        super("Recipe not found: " + id);
    }
}
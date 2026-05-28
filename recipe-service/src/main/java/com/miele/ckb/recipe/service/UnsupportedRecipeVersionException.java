package com.miele.ckb.recipe.service;

public class UnsupportedRecipeVersionException extends RuntimeException {
    public UnsupportedRecipeVersionException(Integer received) {
        super("Unsupported recipe schema version: " + received);
    }
}
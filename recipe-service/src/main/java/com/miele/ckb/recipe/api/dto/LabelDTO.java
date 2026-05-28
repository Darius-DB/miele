package com.miele.ckb.recipe.api.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashMap;
import java.util.Map;

/**
 * Multilingual label, e.g. { "de": "Thorben's Rezept komplett" }.
 *
 * Uses @JsonAnyGetter / @JsonAnySetter so any ISO-639-1 language
 * code is accepted dynamically -- a new language never requires a
 * code change (relevant to Q3: tolerating structural change).
 */
public class LabelDTO {

    @NotEmpty(message = "label must contain at least one translation")
    private final Map<String, String> translations = new HashMap<>();

    @JsonAnySetter
    public void add(String languageCode, String text) {
        translations.put(languageCode, text);
    }

    @JsonAnyGetter
    public Map<String, String> getTranslations() {
        return translations;
    }
}
package com.ascmoda.catalog.application.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

@Component
public class SlugGenerator {

    public String generate(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Slug source must contain at least one letter or digit");
        }

        return normalized;
    }

    public String generateUnique(String baseSlug, Predicate<String> exists) {
        String normalizedBase = generate(baseSlug);
        String candidate = normalizedBase;
        int suffix = 2;

        while (exists.test(candidate)) {
            candidate = normalizedBase + "-" + suffix;
            suffix++;
        }

        return candidate;
    }
}

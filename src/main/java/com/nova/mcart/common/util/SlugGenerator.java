package com.nova.mcart.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugGenerator {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern AMPERSAND = Pattern.compile("&"); // Target the '&'
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]"); // Keeps letters, numbers, and hyphens

    public static String generate(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        // 1. Convert '&' to 'n' (e.g., H&M -> HnM)
        String handleAmpersand = AMPERSAND.matcher(input)
                .replaceAll("n");

        // 2. Replace whitespace with hyphens
        String nowhitespace = WHITESPACE.matcher(handleAmpersand)
                .replaceAll("-");

        // 3. Normalize (handle accents/diacritics)
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);

        // 4. Remove anything that isn't a word character or a hyphen
        String slug = NONLATIN.matcher(normalized)
                .replaceAll("");

        return slug.toLowerCase(Locale.ENGLISH);
    }
}
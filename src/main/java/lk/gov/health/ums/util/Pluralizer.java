package lk.gov.health.ums.util;

/**
 * Naive English pluralization for chart category labels — {@link lk.gov.health.ums.entity.EquipmentType#getName()}
 * is stored singular ("CT Scanner", used in forms/dropdowns/filters), but a chart bar/slice
 * representing a count of that type reads better plural ("CT Scanners"). Covers the regular-noun
 * rules only (sibilant endings, consonant+y); an irregular equipment-type name would need a
 * dedicated override field if one is ever added — not needed for the current fleet.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
public final class Pluralizer {

    private Pluralizer() {
    }

    public static String plural(String noun) {
        if (noun == null || noun.isBlank()) {
            return noun;
        }
        String lower = noun.toLowerCase();
        if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                || lower.endsWith("ch") || lower.endsWith("sh")) {
            return noun + "es";
        }
        if (lower.endsWith("y") && lower.length() > 1 && !isVowel(lower.charAt(lower.length() - 2))) {
            return noun.substring(0, noun.length() - 1) + "ies";
        }
        return noun + "s";
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }

}

package uk.gov.defra.trade.imports.fish.be.configuration;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Accepts only canonical 8-4-4-4-12 hexadecimal UUIDs. {@link UUID#fromString} accepts shortened
 * forms such as {@code 1-1-1-1-1}, which must be rejected as invalid identifiers.
 */
public class StrictUuidEditor extends PropertyEditorSupport {
    private static final Pattern CANONICAL_UUID =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Override
    public void setAsText(String text) {
        if (text == null || !CANONICAL_UUID.matcher(text).matches()) {
            throw new IllegalArgumentException("Invalid UUID format");
        }
        setValue(UUID.fromString(text.toLowerCase(Locale.ROOT)));
    }
}

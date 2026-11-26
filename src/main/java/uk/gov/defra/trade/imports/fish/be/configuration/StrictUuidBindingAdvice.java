package uk.gov.defra.trade.imports.fish.be.configuration;

import java.util.UUID;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Applies strict UUID parsing to path variables, headers and request parameters. A custom editor is
 * used rather than a ConversionService converter, because Spring falls back to its lenient default
 * UUID editor when a converter rejects a value.
 */
@ControllerAdvice
public class StrictUuidBindingAdvice {

    @InitBinder
    public void registerStrictUuidEditor(WebDataBinder binder) {
        binder.registerCustomEditor(UUID.class, new StrictUuidEditor());
    }
}

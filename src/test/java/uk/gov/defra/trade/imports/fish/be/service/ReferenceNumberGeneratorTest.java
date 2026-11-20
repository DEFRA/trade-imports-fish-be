package uk.gov.defra.trade.imports.fish.be.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ReferenceNumberGeneratorTest {
    @Test
    void generatesExactFishReferenceWithFiveCharacterSuffix() {
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(anyInt())).thenReturn(10, 33, 23, 0, 4);
        var generator = new ReferenceNumberGenerator(
            Clock.fixed(Instant.parse("2025-12-31T23:59:59Z"), ZoneOffset.UTC), random);
        assertThat(generator.generate()).isEqualTo("GBN-FIS-25-AXN04");
    }

    @Test
    void newUtcYearIsUsedAtCreation() {
        var generator = new ReferenceNumberGenerator(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), new SecureRandom());
        assertThat(generator.generate()).matches("^GBN-FIS-26-[0-9A-Z]{5}$");
    }
}

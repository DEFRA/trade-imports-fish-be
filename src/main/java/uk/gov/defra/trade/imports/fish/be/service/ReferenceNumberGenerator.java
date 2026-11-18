package uk.gov.defra.trade.imports.fish.be.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Year;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReferenceNumberGenerator {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final Clock clock;
    private final SecureRandom random;

    @Autowired
    public ReferenceNumberGenerator() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    ReferenceNumberGenerator(Clock clock, SecureRandom random) {
        this.clock = clock;
        this.random = random;
    }

    public String generate() {
        StringBuilder suffix = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return String.format(Locale.ROOT, "GBN-FIS-%02d-%s", Year.now(clock).getValue() % 100, suffix);
    }
}

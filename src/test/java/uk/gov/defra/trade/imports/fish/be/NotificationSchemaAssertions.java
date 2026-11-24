package uk.gov.defra.trade.imports.fish.be;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import java.io.IOException;

public final class NotificationSchemaAssertions {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchema AGGREGATE = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        .getSchema(SchemaLocation.of("classpath:/schemas/fis-notification-aggregate.schema.json"),
            SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build());

    private NotificationSchemaAssertions() {}

    public static void assertAggregate(String json) throws IOException {
        assertThat(AGGREGATE.validate(MAPPER.readTree(json))).as("Canonical aggregate schema violations").isEmpty();
    }
}

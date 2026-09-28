package org.quyq.gwsu.common.log.serverfile.domain;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLogSearchRequestTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void shouldSerializeAndDeserializeDateTimeWithSpaceSeparator() throws Exception {
        LocalLogSearchRequest request = new LocalLogSearchRequest(
                "error",
                null,
                LocalDateTime.of(2026, 9, 28, 0, 0, 0),
                LocalDateTime.of(2026, 9, 28, 23, 59, 59),
                10);

        String json = jsonMapper.writeValueAsString(request);

        assertTrue(json.contains("\"startTime\":\"2026-09-28 00:00:00\""));
        assertTrue(json.contains("\"endTime\":\"2026-09-28 23:59:59\""));

        LocalLogSearchRequest deserialized = jsonMapper.readValue(json, LocalLogSearchRequest.class);
        assertEquals(request.startTime(), deserialized.startTime());
        assertEquals(request.endTime(), deserialized.endTime());
    }
}

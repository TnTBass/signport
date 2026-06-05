package tech.endorsed.signport.internal.modstatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModStatusConfigTest {
    @Test
    void configNormalizesClientVersionAndPayloadChannel() {
        ModStatusConfig config = ModStatusConfig.builder()
                .modId("signport")
                .displayName("SignPort")
                .clientVersion("2.2.2+mc26.1.2")
                .updateUrl("https://github.com/TnTBass/signport")
                .payloadChannel("signport", "status_version")
                .build();

        assertEquals("2.2.2", config.clientVersion());
        assertEquals("mc26.1.2", config.clientBuild());
        assertEquals("signport:status_version", config.payloadChannel());
        assertEquals("https://github.com/TnTBass/signport", config.updateUrl());
    }
}

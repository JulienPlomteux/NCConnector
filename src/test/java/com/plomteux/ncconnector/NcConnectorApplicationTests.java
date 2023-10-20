package com.plomteux.ncconnector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ActiveProfiles("test")
@SpringBootTest
class NcConnectorApplicationTests {

    @Test
    void contextLoads() {
        assertDoesNotThrow(() -> NcConnectorApplication.main(new String[]{"--spring.profiles.active=test"}));
    }
}

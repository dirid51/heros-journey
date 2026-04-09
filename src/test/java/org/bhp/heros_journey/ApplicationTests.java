package org.bhp.heros_journey;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
// SonarQube S2068: This hard-coded password is only for test initialization and is not used
// in actual application logic. Test credentials are required to initialize Spring Security context.
@TestPropertySource(properties = {
        "spring.security.user.name=test",
        "spring.security.user.password=test"  // NOSONAR: S2068 - test credential only
})
class ApplicationTests {

    @Test
    void contextLoads() {
        // intentionally left empty
    }

}

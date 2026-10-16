package com.suzhong.njl;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class NjlApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            NjlApplication.main(args);
            spring.verify(() -> SpringApplication.run(NjlApplication.class, args));
        }
    }
}

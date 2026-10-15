package com.suzhong.njl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClasspathGuardTest {

    @Test
    void jpaIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("jakarta.persistence.Entity"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.Session"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.springframework.data.jpa.repository.JpaRepository"));
    }
}

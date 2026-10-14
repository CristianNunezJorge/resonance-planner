package com.resonanceplanner.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UniqueViolationTest {

    @Test
    void recognizesAUniqueViolationAtAnyDepthOfTheCauses() {
        SQLException unique = new SQLException("duplicate key", "23505");

        assertThat(UniqueViolation.isIn(unique)).isTrue();
        assertThat(UniqueViolation.isIn(new DataIntegrityViolationException("x", unique))).isTrue();
        // Hibernate and Spring wrap the error of the driver more than once
        assertThat(UniqueViolation.isIn(new RuntimeException("outer", new IllegalStateException("middle", unique)))).isTrue();
    }

    @Test
    void doesNotTakeOtherErrorsForADuplicate() {
        assertThat(UniqueViolation.isIn(new SQLException("not null", "23502"))).isFalse();
        assertThat(UniqueViolation.isIn(new SQLException("foreign key", "23503"))).isFalse();
        assertThat(UniqueViolation.isIn(new SQLException("no state"))).isFalse();
        assertThat(UniqueViolation.isIn(new RuntimeException("no cause"))).isFalse();
    }
}

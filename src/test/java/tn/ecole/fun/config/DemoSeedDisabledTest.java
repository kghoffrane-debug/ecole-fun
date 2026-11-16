package tn.ecole.fun.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tn.ecole.fun.repo.SchoolRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seed-disabled;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "app.seed-demo=false",
        "app.superadmin-password=test-superadmin-password"
})
@Transactional
class DemoSeedDisabledTest {
    @Autowired private SchoolRepository schools;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void homeworkAndAssessmentsAreNotSeededWhenDemoDataIsDisabled() {
        assertThat(schools.findByDeletedFalseOrderByNameAsc()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM homework", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment", Integer.class)).isZero();
    }
}
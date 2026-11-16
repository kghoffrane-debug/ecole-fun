package tn.ecole.fun.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.ecole.fun.config.DataLoader;
import tn.ecole.fun.model.*;
import tn.ecole.fun.repo.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:homework-assessment-isolation;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "app.seed-demo=true",
        "app.superadmin-password=test-superadmin-password"
})
@AutoConfigureMockMvc
@Transactional
class HomeworkAssessmentIsolationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private SchoolRepository schools;
    @Autowired private SchoolClassRepository classes;
    @Autowired private SubjectRepository subjects;
    @Autowired private AppUserRepository users;
    @Autowired private HomeworkRepository homeworks;
    @Autowired private AssessmentRepository assessments;

    private School schoolA;
    private School schoolB;
    private SchoolClass classA;
    private SchoolClass classB;
    private SchoolClass classAWithoutParent;
    private SchoolClass classBWithoutParent;
    private Subject subjectA;
    private Subject subjectB;
    private Subject subjectNotTaughtByTeacherA;
    private Subject subjectNotTaughtByTeacherB;
    private AppUser teacherA;
    private AppUser teacherB;
    private Homework homeworkA;
    private Homework homeworkB;
    private Assessment assessmentA;
    private Assessment assessmentB;

    @BeforeEach
    void setUp() {
        schoolA = schools.findFirstByName(DataLoader.SCHOOL_A).orElseThrow();
        schoolB = schools.findFirstByName(DataLoader.SCHOOL_B).orElseThrow();
        teacherA = users.findByUsernameAndDeletedFalse("enseignant").orElseThrow();
        teacherB = users.findByUsernameAndDeletedFalse("enseignant-b").orElseThrow();
        classA = findClass(schoolA, "CE1");
        classB = findClass(schoolB, "CE1");
        classAWithoutParent = findClass(schoolA, "CE2");
        classBWithoutParent = findClass(schoolB, "CP");
        subjectA = findSubject(schoolA, "Mathématiques");
        subjectB = findSubject(schoolB, "Mathématiques");
        homeworkA = findHomework(schoolA, "A-Devoir 1");
        homeworkB = findHomework(schoolB, "B-Devoir 1");
        assessmentA = findAssessment(schoolA, "A-Contrôle 1");
        assessmentB = findAssessment(schoolB, "B-Contrôle 1");

        subjectNotTaughtByTeacherA = createSubject(schoolA, createTeacher(schoolA, "autre-enseignant-a"),
                "A-Matière autre enseignant");
        subjectNotTaughtByTeacherB = createSubject(schoolB, createTeacher(schoolB, "autre-enseignant-b"),
                "B-Matière autre enseignant");

        createHomework(schoolA, classAWithoutParent, subjectA, teacherA, "A-Devoir classe non enfant");
        createHomework(schoolB, classBWithoutParent, subjectB, teacherB, "B-Devoir classe non enfant");
        createAssessment(schoolA, classAWithoutParent, subjectA, teacherA, "A-Contrôle classe non enfant");
        createAssessment(schoolB, classBWithoutParent, subjectB, teacherB, "B-Contrôle classe non enfant");
    }

    @Test
    void studentsSeeOnlyTheirOwnSchoolInBothDirections() throws Exception {
        String homeworkAHtml = mockMvc.perform(get("/homework").with(user("eleve").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String assessmentsAHtml = mockMvc.perform(get("/assessments").with(user("eleve").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(homeworkAHtml).contains("A-Devoir 1").doesNotContain("B-Devoir 1");
        assertThat(assessmentsAHtml).contains("A-Contrôle 1").doesNotContain("B-Contrôle 1");

        String homeworkBHtml = mockMvc.perform(get("/homework").with(user("eleve-b").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String assessmentsBHtml = mockMvc.perform(get("/assessments").with(user("eleve-b").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(homeworkBHtml).contains("B-Devoir 1").doesNotContain("A-Devoir 1");
        assertThat(assessmentsBHtml).contains("B-Contrôle 1").doesNotContain("A-Contrôle 1");
    }

    @Test
    void teachersCannotPublishForAnotherTeachersSubjectInEitherSchool() throws Exception {
        assertTeacherCannotUseSubject("enseignant", classA, subjectNotTaughtByTeacherA);
        assertTeacherCannotUseSubject("enseignant-b", classB, subjectNotTaughtByTeacherB);
    }

    @Test
    void openingAnotherSchoolsHomeworkReturnsNotFoundInBothDirections() throws Exception {
        mockMvc.perform(get("/homework/{id}", homeworkB.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/homework/{id}", homeworkA.getId()).with(user("admin-b").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void foreignSchoolIdInPostDoesNotChangeTheOwningSchoolInEitherDirection() throws Exception {
        createWithForeignSchoolId("admin", schoolA, schoolB, classA, subjectA, teacherA, "A-POST-school-spoof");
        createWithForeignSchoolId("admin-b", schoolB, schoolA, classB, subjectB, teacherB, "B-POST-school-spoof");
    }

    @Test
    void parentsSeeOnlyHomeworkAndAssessmentsForTheirChildrenClassesInBothDirections() throws Exception {
        String homeworkAHtml = mockMvc.perform(get("/homework").with(user("parent").roles("PARENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String assessmentsAHtml = mockMvc.perform(get("/assessments").with(user("parent").roles("PARENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(homeworkAHtml).contains("A-Devoir 1").doesNotContain("A-Devoir classe non enfant", "B-Devoir 1");
        assertThat(assessmentsAHtml).contains("A-Contrôle 1")
                .doesNotContain("A-Contrôle classe non enfant", "B-Contrôle 1");

        String homeworkBHtml = mockMvc.perform(get("/homework").with(user("parent-b").roles("PARENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String assessmentsBHtml = mockMvc.perform(get("/assessments").with(user("parent-b").roles("PARENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(homeworkBHtml).contains("B-Devoir 1").doesNotContain("B-Devoir classe non enfant", "A-Devoir 1");
        assertThat(assessmentsBHtml).contains("B-Contrôle 1")
                .doesNotContain("B-Contrôle classe non enfant", "A-Contrôle 1");
    }

    private void assertTeacherCannotUseSubject(String username, SchoolClass schoolClass, Subject otherSubject)
            throws Exception {
        mockMvc.perform(post("/homework/save")
                        .with(user(username).roles("TEACHER"))
                        .with(csrf())
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("subjectId", otherSubject.getId().toString())
                        .param("title", "Unauthorized homework")
                        .param("dueDate", LocalDate.now().plusDays(4).toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/assessments/save")
                        .with(user(username).roles("TEACHER"))
                        .with(csrf())
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("subjectId", otherSubject.getId().toString())
                        .param("type", AssessmentType.CONTROLE.name())
                        .param("date", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isNotFound());
    }

    private void createWithForeignSchoolId(String username, School ownSchool, School foreignSchool,
                                          SchoolClass schoolClass, Subject subject, AppUser expectedTeacher,
                                          String marker) throws Exception {
        mockMvc.perform(post("/homework/save")
                        .with(user(username).roles("ADMIN"))
                        .with(csrf())
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("subjectId", subject.getId().toString())
                        .param("title", marker + " homework")
                        .param("dueDate", LocalDate.now().plusDays(6).toString())
                        .param("school_id", foreignSchool.getId().toString())
                        .param("teacher_id", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/homework"));

        Homework savedHomework = findHomework(ownSchool, marker + " homework");
        assertThat(savedHomework.getSchool().getId()).isEqualTo(ownSchool.getId());
        assertThat(savedHomework.getTeacher().getId()).isEqualTo(expectedTeacher.getId());

        mockMvc.perform(post("/assessments/save")
                        .with(user(username).roles("ADMIN"))
                        .with(csrf())
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("subjectId", subject.getId().toString())
                        .param("type", AssessmentType.EXAMEN.name())
                        .param("date", LocalDate.now().plusDays(7).toString())
                        .param("notes", marker + " assessment")
                        .param("school_id", foreignSchool.getId().toString())
                        .param("teacher_id", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assessments"));

        Assessment savedAssessment = assessments.findBySchoolAndDeletedFalseOrderByDateAscStartTimeAsc(ownSchool)
                .stream().filter(item -> (marker + " assessment").equals(item.getNotes())).findFirst().orElseThrow();
        assertThat(savedAssessment.getSchool().getId()).isEqualTo(ownSchool.getId());
        assertThat(savedAssessment.getTeacher().getId()).isEqualTo(expectedTeacher.getId());
    }

    private SchoolClass findClass(School school, String level) {
        return classes.findBySchoolAndDeletedFalseOrderByLevelAscSectionAsc(school).stream()
                .filter(schoolClass -> level.equals(schoolClass.getLevel()))
                .findFirst().orElseThrow();
    }

    private Subject findSubject(School school, String name) {
        return subjects.findBySchoolAndDeletedFalseOrderByNameAsc(school).stream()
                .filter(subject -> name.equals(subject.getName()))
                .findFirst().orElseThrow();
    }

    private Homework findHomework(School school, String title) {
        return homeworks.findBySchoolAndDeletedFalseOrderByDueDateAsc(school).stream()
                .filter(homework -> title.equals(homework.getTitle()))
                .findFirst().orElseThrow();
    }

    private Assessment findAssessment(School school, String notes) {
        return assessments.findBySchoolAndDeletedFalseOrderByDateAscStartTimeAsc(school).stream()
                .filter(assessment -> notes.equals(assessment.getNotes()))
                .findFirst().orElseThrow();
    }

    private AppUser createTeacher(School school, String username) {
        AppUser teacher = new AppUser();
        teacher.setSchool(school);
        teacher.setUsername(username);
        teacher.setPassword("test-password");
        teacher.setFullName(username);
        teacher.setRole(Role.TEACHER);
        return users.save(teacher);
    }

    private Subject createSubject(School school, AppUser teacher, String name) {
        Subject subject = new Subject();
        subject.setSchool(school);
        subject.setTeacher(teacher);
        subject.setName(name);
        subject.setCoefficient(1);
        subject.setLevel("Tous");
        return subjects.save(subject);
    }

    private Homework createHomework(School school, SchoolClass schoolClass, Subject subject,
                                    AppUser teacher, String title) {
        Homework homework = new Homework();
        homework.setSchool(school);
        homework.setSchoolClass(schoolClass);
        homework.setSubject(subject);
        homework.setTeacher(teacher);
        homework.setTitle(title);
        homework.setInstructions(title);
        homework.setDueDate(LocalDate.now().plusDays(8));
        homework.setCreatedAt(LocalDateTime.now());
        return homeworks.save(homework);
    }

    private Assessment createAssessment(School school, SchoolClass schoolClass, Subject subject,
                                        AppUser teacher, String notes) {
        Assessment assessment = new Assessment();
        assessment.setSchool(school);
        assessment.setSchoolClass(schoolClass);
        assessment.setSubject(subject);
        assessment.setTeacher(teacher);
        assessment.setType(AssessmentType.CONTROLE);
        assessment.setDate(LocalDate.now().plusDays(9));
        assessment.setStartTime(LocalTime.of(10, 0));
        assessment.setNotes(notes);
        return assessments.save(assessment);
    }
}
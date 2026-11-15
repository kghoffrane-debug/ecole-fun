package tn.ecole.fun.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.ecole.fun.model.*;
import tn.ecole.fun.repo.*;
import tn.ecole.fun.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Crée le compte propriétaire du SaaS et, en mode démo, DEUX écoles fictives (A et B)
 * avec des noms reconnaissables, pour pouvoir vérifier l'isolation à la main et par les tests.
 */
@Component
public class DataLoader implements CommandLineRunner {
    public static final String SCHOOL_A = "École Les Palmiers";
    public static final String SCHOOL_B = "École Les Oliviers";

    private final SchoolRepository schools;
    private final AppUserRepository users;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final GradeRepository grades;
    private final PaymentService payments;
    private final PasswordEncoder encoder;

    @Value("${app.seed-demo:true}")
    private boolean seedDemo;
    @Value("${app.superadmin-password}")
    private String superAdminPassword;
    @Value("${app.annee-scolaire}")
    private String annee;

    public DataLoader(SchoolRepository schools, AppUserRepository users, SchoolClassRepository classes,
                      SubjectRepository subjects, StudentRepository students, GradeRepository grades,
                      PaymentService payments, PasswordEncoder encoder) {
        this.schools = schools;
        this.users = users;
        this.classes = classes;
        this.subjects = subjects;
        this.students = students;
        this.grades = grades;
        this.payments = payments;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        // Le propriétaire du SaaS n'est rattaché à aucune école.
        user(null, "superadmin", superAdminPassword, "Propriétaire du SaaS", Role.SUPER_ADMIN);
        if (!seedDemo) return;

        seedSchoolA(school(SCHOOL_A, School.ACTIVE, "premium"));
        seedSchoolB(school(SCHOOL_B, School.TRIAL, "basic"));
    }

    // ---------------------------------------------------------------- École A
    private void seedSchoolA(School school) {
        user(school, "admin", "admin123", "Mme Salma Ben Ali", Role.ADMIN);
        AppUser teacher = user(school, "enseignant", "ens123", "M. Karim Trabelsi", Role.TEACHER);
        AppUser parent = user(school, "parent", "parent123", "Mme Leila Gharbi", Role.PARENT);
        AppUser accountant = user(school, "compta", "compta123", "M. Sami Jlassi", Role.ACCOUNTANT);
        AppUser pupil = user(school, "eleve", "eleve123", "Yassine Gharbi", Role.STUDENT);

        SchoolClass cp = schoolClass(school, "CP", "A", teacher);
        SchoolClass ce1 = schoolClass(school, "CE1", "A", teacher);
        schoolClass(school, "CE2", "A", null);

        Subject maths = subject(school, "Mathématiques", 3, teacher);
        Subject french = subject(school, "Français", 3, teacher);
        Subject arabic = subject(school, "Arabe", 3, teacher);
        Subject science = subject(school, "Éveil scientifique", 2, teacher);
        Subject english = subject(school, "Anglais", 1, teacher);

        Student yassine = student(school, "Yassine", "Gharbi", "🦊", LocalDate.of(2018, 4, 12), parent, pupil, ce1);
        Student lina = student(school, "Lina", "Gharbi", "🐼", LocalDate.of(2020, 9, 3), parent, null, cp);
        student(school, "Adam", "Mansour", "🐯", LocalDate.of(2018, 11, 21), null, null, ce1);

        grade(school, yassine, maths, "Contrôle 1", 17);
        grade(school, yassine, maths, "Contrôle 2", 20);
        grade(school, yassine, french, "Dictée", 15);
        grade(school, yassine, arabic, "Lecture", 14);
        grade(school, yassine, science, "Les plantes", 18);
        grade(school, yassine, english, "Vocabulaire", 16);
        grade(school, lina, maths, "Les nombres", 12);
        grade(school, lina, french, "Les lettres", 13.5);

        pay(yassine, "400", accountant);
    }

    // ---------------------------------------------------------------- École B
    private void seedSchoolB(School school) {
        user(school, "admin-b", "admin123", "M. Hatem Ouali", Role.ADMIN);
        AppUser teacher = user(school, "enseignant-b", "ens123", "Mme Rim Dridi", Role.TEACHER);
        AppUser parent = user(school, "parent-b", "parent123", "M. Walid Bouzid", Role.PARENT);
        AppUser accountant = user(school, "compta-b", "compta123", "Mme Hela Sassi", Role.ACCOUNTANT);
        AppUser pupil = user(school, "eleve-b", "eleve123", "Sarra Bouzid", Role.STUDENT);

        SchoolClass cp = schoolClass(school, "CP", "B", teacher);
        SchoolClass ce1 = schoolClass(school, "CE1", "B", teacher);

        Subject maths = subject(school, "Mathématiques", 3, teacher);
        Subject french = subject(school, "Français", 3, teacher);

        Student sarra = student(school, "Sarra", "Bouzid", "🐰", LocalDate.of(2018, 6, 2), parent, pupil, ce1);
        Student omar = student(school, "Omar", "Chaabane", "🐙", LocalDate.of(2020, 1, 15), null, null, cp);

        grade(school, sarra, maths, "Contrôle 1", 15);
        grade(school, sarra, french, "Dictée", 18);
        grade(school, omar, maths, "Les nombres", 11);

        pay(sarra, "250", accountant);
    }

    // ---------------------------------------------------------------- aides
    private School school(String name, String status, String plan) {
        School s = new School();
        s.setName(name);
        s.setStatus(status);
        s.setPlan(plan);
        return schools.save(s);
    }

    private AppUser user(School school, String username, String pwd, String fullName, Role role) {
        AppUser u = new AppUser();
        u.setSchool(school);
        u.setUsername(username);
        u.setPassword(encoder.encode(pwd));
        u.setFullName(fullName);
        u.setRole(role);
        return users.save(u);
    }

    private SchoolClass schoolClass(School school, String level, String section, AppUser teacher) {
        SchoolClass c = new SchoolClass();
        c.setSchool(school);
        c.setLevel(level);
        c.setSection(section);
        c.setCapacity(25);
        c.setAnneeScolaire(annee);
        c.setTeacher(teacher);
        return classes.save(c);
    }

    private Subject subject(School school, String name, double coef, AppUser teacher) {
        Subject s = new Subject();
        s.setSchool(school);
        s.setName(name);
        s.setCoefficient(coef);
        s.setLevel("Tous");
        s.setTeacher(teacher);
        return subjects.save(s);
    }

    private Student student(School school, String first, String last, String avatar, LocalDate birth,
                            AppUser parent, AppUser login, SchoolClass c) {
        Student s = new Student();
        s.setSchool(school);
        s.setFirstName(first);
        s.setLastName(last);
        s.setAvatar(avatar);
        s.setBirthDate(birth);
        s.setParent(parent);
        s.setUser(login);
        s.setSchoolClass(c);
        s.setAnneeScolaire(annee);
        s.setTuitionDue(new BigDecimal("1200"));
        return students.save(s);
    }

    private void grade(School school, Student st, Subject sub, String title, double score) {
        Grade g = new Grade();
        g.setSchool(school);
        g.setStudent(st);
        g.setSubject(sub);
        g.setTitle(title);
        g.setScore(score);
        g.setTrimester(1);
        g.setAnneeScolaire(annee);
        g.setGradedOn(LocalDate.now());
        grades.save(g);
    }

    private void pay(Student st, String amount, AppUser accountant) {
        Payment p = new Payment();
        p.setStudent(st);
        p.setAmount(new BigDecimal(amount));
        p.setMethod(PayMethod.CASH);
        p.setPaidAt(LocalDate.now());
        payments.register(p, accountant);
    }
}

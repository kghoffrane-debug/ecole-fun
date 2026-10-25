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

/** Crée le compte administrateur et, en mode démo, un jeu de données joyeux. */
@Component
public class DataLoader implements CommandLineRunner {
    private final AppUserRepository users;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final GradeRepository grades;
    private final PaymentService payments;
    private final PasswordEncoder encoder;

    @Value("${app.seed-demo:true}")
    private boolean seedDemo;
    @Value("${app.admin-password:admin123}")
    private String adminPassword;
    @Value("${app.annee-scolaire}")
    private String annee;

    public DataLoader(AppUserRepository users, SchoolClassRepository classes, SubjectRepository subjects,
                      StudentRepository students, GradeRepository grades, PaymentService payments,
                      PasswordEncoder encoder) {
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

        AppUser admin = user("admin", adminPassword, "Mme Salma Ben Ali", Role.ADMIN);
        if (!seedDemo) return;

        AppUser teacher = user("enseignant", "ens123", "M. Karim Trabelsi", Role.TEACHER);
        AppUser parent = user("parent", "parent123", "Mme Leila Gharbi", Role.PARENT);
        AppUser accountant = user("compta", "compta123", "M. Sami Jlassi", Role.ACCOUNTANT);
        AppUser pupil = user("eleve", "eleve123", "Yassine Gharbi", Role.STUDENT);

        SchoolClass cp = schoolClass("CP", "A", teacher);
        SchoolClass ce1 = schoolClass("CE1", "A", teacher);
        schoolClass("CE2", "A", null);

        Subject maths = subject("Mathématiques", 3, teacher);
        Subject french = subject("Français", 3, teacher);
        Subject arabic = subject("Arabe", 3, teacher);
        Subject science = subject("Éveil scientifique", 2, teacher);
        Subject english = subject("Anglais", 1, teacher);

        Student yassine = student("Yassine", "Gharbi", "🦊", LocalDate.of(2018, 4, 12), parent, pupil, ce1);
        Student lina = student("Lina", "Gharbi", "🐼", LocalDate.of(2020, 9, 3), parent, null, cp);
        student("Adam", "Mansour", "🐯", LocalDate.of(2018, 11, 21), null, null, ce1);

        grade(yassine, maths, "Contrôle 1", 17, 1);
        grade(yassine, maths, "Contrôle 2", 20, 1);
        grade(yassine, french, "Dictée", 15, 1);
        grade(yassine, arabic, "Lecture", 14, 1);
        grade(yassine, science, "Les plantes", 18, 1);
        grade(yassine, english, "Vocabulaire", 16, 1);
        grade(lina, maths, "Les nombres", 12, 1);
        grade(lina, french, "Les lettres", 13.5, 1);

        Payment p = new Payment();
        p.setStudent(yassine);
        p.setAmount(new BigDecimal("400"));
        p.setMethod(PayMethod.CASH);
        p.setPaidAt(LocalDate.now());
        payments.register(p, accountant);
    }

    private AppUser user(String username, String pwd, String fullName, Role role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(encoder.encode(pwd));
        u.setFullName(fullName);
        u.setRole(role);
        return users.save(u);
    }

    private SchoolClass schoolClass(String level, String section, AppUser teacher) {
        SchoolClass c = new SchoolClass();
        c.setLevel(level);
        c.setSection(section);
        c.setCapacity(25);
        c.setAnneeScolaire(annee);
        c.setTeacher(teacher);
        return classes.save(c);
    }

    private Subject subject(String name, double coef, AppUser teacher) {
        Subject s = new Subject();
        s.setName(name);
        s.setCoefficient(coef);
        s.setLevel("Tous");
        s.setTeacher(teacher);
        return subjects.save(s);
    }

    private Student student(String first, String last, String avatar, LocalDate birth,
                            AppUser parent, AppUser login, SchoolClass c) {
        Student s = new Student();
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

    private void grade(Student st, Subject sub, String title, double score, int trimester) {
        Grade g = new Grade();
        g.setStudent(st);
        g.setSubject(sub);
        g.setTitle(title);
        g.setScore(score);
        g.setTrimester(trimester);
        g.setAnneeScolaire(annee);
        g.setGradedOn(LocalDate.now());
        grades.save(g);
    }
}

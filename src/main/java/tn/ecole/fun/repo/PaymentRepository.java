package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.Payment;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.Student;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySchoolAndDeletedFalseOrderByPaidAtDescIdDesc(School school);

    Optional<Payment> findByIdAndSchoolAndDeletedFalse(Long id, School school);

    List<Payment> findByStudentAndDeletedFalseOrderByPaidAtDescIdDesc(Student student);
}
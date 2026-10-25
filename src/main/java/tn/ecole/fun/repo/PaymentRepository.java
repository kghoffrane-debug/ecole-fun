package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.Payment;
import tn.ecole.fun.model.Student;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByDeletedFalseOrderByPaidAtDescIdDesc();

    List<Payment> findByStudentAndDeletedFalseOrderByPaidAtDescIdDesc(Student student);
}

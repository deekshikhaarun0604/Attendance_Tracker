package com.smartattend.repository;

import com.smartattend.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByBarcodeValue(String barcodeValue);
    Optional<Student> findByRegisterNumber(String registerNumber);
    Optional<Student> findByStudentId(String studentId);
    Optional<Student> findByEmail(String email);

    Boolean existsByRegisterNumber(String registerNumber);
    Boolean existsByStudentId(String studentId);
    Boolean existsByEmail(String email);
    Boolean existsByBarcodeValue(String barcodeValue);

    List<Student> findByDepartmentAndYearOfStudyAndSection(String department, Integer yearOfStudy, String section);
    List<Student> findByBarcodeRegistered(boolean barcodeRegistered);
    List<Student> findByStatus(String status);

    long countByBarcodeRegistered(boolean barcodeRegistered);
    long countByStatus(String status);

    @Query("{ $or: [ " +
           "{ 'studentName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'registerNumber': { $regex: ?0, $options: 'i' } }, " +
           "{ 'studentId': { $regex: ?0, $options: 'i' } }, " +
           "{ 'barcodeValue': { $regex: ?0, $options: 'i' } } ] }")
    List<Student> searchStudents(String query);
}

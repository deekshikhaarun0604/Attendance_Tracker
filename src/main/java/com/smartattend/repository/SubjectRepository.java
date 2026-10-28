package com.smartattend.repository;

import com.smartattend.model.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends MongoRepository<Subject, String> {
    Optional<Subject> findBySubjectCode(String subjectCode);
    List<Subject> findByDepartment(String department);
    List<Subject> findByDepartmentAndYearOfStudy(String department, Integer yearOfStudy);
    boolean existsBySubjectCode(String subjectCode);
}

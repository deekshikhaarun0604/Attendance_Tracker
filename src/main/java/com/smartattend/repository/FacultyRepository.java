package com.smartattend.repository;

import com.smartattend.model.Faculty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends MongoRepository<Faculty, String> {
    Optional<Faculty> findByEmployeeId(String employeeId);
    Optional<Faculty> findByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
}

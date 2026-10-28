package com.smartattend.repository;

import com.smartattend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Optional<User> findByLinkedStudentId(String linkedStudentId);
    Optional<User> findByLinkedFacultyId(String linkedFacultyId);
}

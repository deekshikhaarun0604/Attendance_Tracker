package com.smartattend.repository;

import com.smartattend.model.AttendanceAnomaly;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceAnomalyRepository extends MongoRepository<AttendanceAnomaly, String> {
    List<AttendanceAnomaly> findByResolvedFalseOrderByTimestampDesc();
    List<AttendanceAnomaly> findAllByOrderByTimestampDesc();
    long countByResolvedFalse();
}

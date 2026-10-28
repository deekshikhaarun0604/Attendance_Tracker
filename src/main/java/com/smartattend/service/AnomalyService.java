package com.smartattend.service;

import com.smartattend.model.AttendanceAnomaly;
import com.smartattend.repository.AttendanceAnomalyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnomalyService {

    private final AttendanceAnomalyRepository anomalyRepository;

    public AnomalyService(AttendanceAnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    public AttendanceAnomaly logAnomaly(String attendanceSessionId, String studentId, String registerNumber,
                                        String studentName, String scannedBarcodeValue, String anomalyType,
                                        String severity, String description) {
        AttendanceAnomaly anomaly = new AttendanceAnomaly(
                attendanceSessionId,
                studentId,
                registerNumber,
                studentName,
                scannedBarcodeValue,
                anomalyType,
                severity,
                description
        );
        return anomalyRepository.save(anomaly);
    }

    public List<AttendanceAnomaly> getUnresolvedAnomalies() {
        return anomalyRepository.findByResolvedFalseOrderByTimestampDesc();
    }

    public List<AttendanceAnomaly> getAllAnomalies() {
        return anomalyRepository.findAllByOrderByTimestampDesc();
    }

    public AttendanceAnomaly resolveAnomaly(String id, String resolutionNotes) {
        AttendanceAnomaly anomaly = anomalyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found with ID: " + id));
        anomaly.setResolved(true);
        anomaly.setResolutionNotes(resolutionNotes);
        return anomalyRepository.save(anomaly);
    }

    public long getActiveAnomalyCount() {
        return anomalyRepository.countByResolvedFalse();
    }
}

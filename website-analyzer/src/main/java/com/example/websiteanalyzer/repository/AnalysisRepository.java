package com.example.websiteanalyzer.repository;

import com.example.websiteanalyzer.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisRepository extends JpaRepository<AnalysisResult, Long> {}

package com.adobe.printservice.repository;

import com.adobe.printservice.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, String> {
}

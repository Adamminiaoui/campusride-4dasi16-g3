package tn.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.userservice.entity.University;

public interface UniversityRepository extends JpaRepository<University, Long> {
}
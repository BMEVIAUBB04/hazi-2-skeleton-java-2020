package hu.bme.aut.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.bme.aut.logistics.model.Milestone;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

}

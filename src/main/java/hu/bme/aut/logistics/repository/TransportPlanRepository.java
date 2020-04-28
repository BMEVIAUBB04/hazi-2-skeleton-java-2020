package hu.bme.aut.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.bme.aut.logistics.model.TransportPlan;

public interface TransportPlanRepository extends JpaRepository<TransportPlan, Long> {

}

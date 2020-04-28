package hu.bme.aut.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.bme.aut.logistics.model.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {

}

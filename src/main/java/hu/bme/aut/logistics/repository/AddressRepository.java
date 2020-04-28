package hu.bme.aut.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.bme.aut.logistics.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
    
}

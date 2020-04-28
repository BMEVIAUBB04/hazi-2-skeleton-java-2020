package hu.bme.aut.logistics.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import hu.bme.aut.logistics.model.Address;
import hu.bme.aut.logistics.model.Milestone;
import hu.bme.aut.logistics.model.Section;
import hu.bme.aut.logistics.model.TransportPlan;
import hu.bme.aut.logistics.repository.AddressRepository;
import hu.bme.aut.logistics.repository.MilestoneRepository;
import hu.bme.aut.logistics.repository.SectionRepository;
import hu.bme.aut.logistics.repository.TransportPlanRepository;


@Component
public class TestDataHelper {

    @Autowired
    AddressRepository addressRepository;
    
    @Autowired
    SectionRepository sectionRepository;
    
    @Autowired
    MilestoneRepository milestoneRepository;

    @Autowired
    TransportPlanRepository transportPlanRepository;
    
    @Autowired
    JdbcTemplate jdbcTemplate;

    public void clearDb() {
        jdbcTemplate.update("DELETE FROM section");
        jdbcTemplate.update("DELETE FROM milestone");
        jdbcTemplate.update("DELETE FROM address");
        jdbcTemplate.update("DELETE FROM transport_plan");
    }
    
    public Address insertTestAddress() {
        Address address = createTestAddress();
        addressRepository.save(address);
        return address;
    }
    
    public Address insertTestAddress(Address address) {
        addressRepository.save(address);
        return address;
    }

    public Address createTestAddress() {
        Address address = new Address();
        address.setCountry("HU");
        address.setCity("Budapest");
        address.setGeoLat(47.497913);
        address.setGeoLng(19.040236);
        address.setNumber("2");
        address.setStreet("Magyar tudósok körútja");
        address.setZipCode("1111");
        return address;
    }
    
    
    public List<TransportPlan> insertTransportPlans() {
        LocalDateTime oldDateTime = LocalDateTime.of(2019, 1, 30, 12, 30, 0);

        TransportPlan transportPlan1 = createPlanWithLastPlannedTime(oldDateTime);

        LocalDateTime newDateTime = LocalDateTime.of(2020, 10, 30, 12, 30, 0);
        TransportPlan transportPlan2 = createPlanWithLastPlannedTime(newDateTime);

        return List.of(transportPlan1, transportPlan2);
    }

    private TransportPlan createPlanWithLastPlannedTime(LocalDateTime plannedDateTime) {
        List<Section> sections = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Milestone from = milestoneRepository.save(new Milestone(null, plannedDateTime.minusDays(1)));
            Milestone to = milestoneRepository.save(new Milestone(null, plannedDateTime));
            Section section = new Section(from, to, i);
            section = sectionRepository.save(section);
            sections.add(section);
        }
        TransportPlan transportPlan = transportPlanRepository.save(new TransportPlan());
        sections.forEach(section -> {
            section.setTransportPlan(transportPlan);
            Section managedSection = sectionRepository.save(section);
            transportPlan.addSection(managedSection);
        });
        return transportPlan;
    }

}

package hu.bme.aut.logistics.model;

import java.util.ArrayList;

import java.util.List;
import javax.persistence.*;

@Entity
public class TransportPlan {

    @Id
    @GeneratedValue
    private Long id;
    
	@OneToMany(mappedBy = "transportPlan")
	@OrderBy("number")
	private java.util.List<Section> sections;
	
	public TransportPlan() {
	}

    public TransportPlan(List<Section> sections) {
        this.sections = sections;
        sections.forEach(s -> {s.setTransportPlan(this);});
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
	public java.util.List<Section> getSections() {
		return sections;
	}

	public void setSections(java.util.List<Section> sections) {
		this.sections = sections;
	}

    public void addSection(Section section) {
        if(sections == null)
            sections = new ArrayList<>();
        sections.add(section);
        section.setTransportPlan(this);
        
    }
}

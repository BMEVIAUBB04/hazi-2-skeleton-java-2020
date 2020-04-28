package hu.bme.aut.logistics.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
public class Section {
    
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
	private Milestone fromMilestone;
    
    @ManyToOne
	private Milestone toMilestone;
    
    @ManyToOne
    private TransportPlan transportPlan;
    
    private int number;

	public Section() {
	}

    public Section(Milestone fromMilestone, Milestone toMilestone, int number) {
        this.fromMilestone = fromMilestone;
        this.toMilestone = toMilestone;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Milestone getFromMilestone() {
        return fromMilestone;
    }

    public void setFromMilestone(Milestone fromMilestone) {
        this.fromMilestone = fromMilestone;
    }

    public Milestone getToMilestone() {
        return toMilestone;
    }

    public void setToMilestone(Milestone toMilestone) {
        this.toMilestone = toMilestone;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public TransportPlan getTransportPlan() {
        return transportPlan;
    }

    public void setTransportPlan(TransportPlan transportPlan) {
        this.transportPlan = transportPlan;
    }

}

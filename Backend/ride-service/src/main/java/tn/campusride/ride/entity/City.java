package tn.campusride.ride.entity;

import jakarta.persistence.*;

/** Table de reference cities (rapport v4) : aucune donnee d'un autre microservice. */
@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(name = "uk_cities_name", columnNames = "name"))
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 100)
    private String governorate;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGovernorate() { return governorate; }
    public void setGovernorate(String governorate) { this.governorate = governorate; }
}

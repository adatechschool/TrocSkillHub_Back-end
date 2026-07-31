package RNCP.TrocSkillHub.Models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = true)
    private String name;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "links", nullable = true, columnDefinition = "TEXT")
    private String links;

    @Column(name = "date_start", nullable = true)
    private LocalDate dateStart;

    @Column(name = "date_end", nullable = true)
    private LocalDate dateEnd;

    // Relation with User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructor required by JPA
    public Project() {
    }

    // Constructor with fields
    public Project(String name, String description, String links, LocalDate dateStart, LocalDate dateEnd, User user) {
        this.name = name;
        this.description = description;
        this.links = links;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.user = user;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLinks() {
        return links;
    }

    public LocalDate getDateStart() {
        return dateStart;
    }

    public LocalDate getDateEnd() {
        return dateEnd;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public void setDateStart(LocalDate dateStart) {
        this.dateStart = dateStart;
    }

    public void setDateEnd(LocalDate dateEnd) {
        this.dateEnd = dateEnd;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", links='" + links + '\'' +
                ", dateStart=" + dateStart +
                ", dateEnd=" + dateEnd +
                '}';
    }
}

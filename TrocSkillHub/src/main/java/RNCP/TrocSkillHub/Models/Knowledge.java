package RNCP.TrocSkillHub.Models;



import java.util.List;

import jakarta.persistence.*;


@Entity
@Table(name = "knowledge")
public class Knowledge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "category_id")
    private Long categoryId;

    @OneToMany(mappedBy = "knowledge", cascade = CascadeType.ALL)
    private List<UserKnowledge> userKnowledge;

    public Knowledge() {}
  public Knowledge(Long id, String name, Long categoryId, List<UserKnowledge> userKnowledge) {
      this.id = id;
      this.name = name;
      this.categoryId = categoryId;
      this.userKnowledge = userKnowledge;
  }
  // Getters
  public Long getId() {
      return id;
  }
  public String getName() {
      return name;
  }
  public Long getCategoryId() {
      return categoryId;
  }
  // Setters
  public void setId(Long id) {
      this.id = id;
  }
  public void setName(String name) {
      this.name = name;
  }
  public void setCategoryId(Long categoryId) {
      this.categoryId = categoryId;
  }

  public List<UserKnowledge> getUserKnowledge() {
    return userKnowledge;
  }

public void setUserKnowledge(List<UserKnowledge> userKnowledge) {
    this.userKnowledge = userKnowledge;
}
  @Override
  public String toString() {
      return "Knowledge{" +
              "id=" + id +
              ", name='" + name + '\'' +
              ", categoryId=" + categoryId +
              '}';
  }
}
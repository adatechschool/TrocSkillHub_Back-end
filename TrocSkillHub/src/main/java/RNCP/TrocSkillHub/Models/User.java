package RNCP.TrocSkillHub.Models;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Lob
    @Column(nullable = true)
    private byte[] picture;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String address;
    
    @Column(nullable = true)
    private String city;
    
    @Column(name = "Country", nullable = true)
    private String country;
    
    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;
    
    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;  
    
    // Getters
    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public byte[] getPicture() {
        return picture;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Integer id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}

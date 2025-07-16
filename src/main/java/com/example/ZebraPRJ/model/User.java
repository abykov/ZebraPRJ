package com.example.ZebraPRJ.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "email", nullable = false)
    private String email;
    @Column(name = "birthdate", nullable = false)
    private LocalDate birthdate;

    // Конструктор по умолчанию (требуется для JPA)
    public User() {
    }

    public User(Long id, String name, String email, LocalDate birthdate){
        this.id = id;
        this.name = name;
        this.email = email;
        this.birthdate = birthdate;
    }
    // Геттеры и сеттеры
    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public LocalDate getBirthdate(){
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate){
        this.birthdate = birthdate;
    }
}

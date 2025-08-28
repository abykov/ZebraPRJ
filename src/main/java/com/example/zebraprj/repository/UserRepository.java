package com.example.zebraprj.repository;

import com.example.zebraprj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByName(String name);
    boolean existsByEmail(String email);

    List<User> findByName(String name);
}


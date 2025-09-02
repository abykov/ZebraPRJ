package com.example.zebraprj.repository;

import com.example.zebraprj.model.UserProperty;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserPropertyRepository extends MongoRepository<UserProperty, String> {
}

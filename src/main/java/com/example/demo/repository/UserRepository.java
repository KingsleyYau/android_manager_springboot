package com.example.demo.repository;

import com.example.demo.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 按年龄范围和用户名包含关键字查询用户
    @Query("SELECT u FROM User u WHERE u.age BETWEEN :minAge AND :maxAge AND u.username LIKE %:keyword%")
    List<User> findByAgeRangeAndUsernameContaining(
        @Param("minAge") Integer minAge,
        @Param("maxAge") Integer maxAge,
        @Param("keyword") String keyword);
}
package com.example.toychat.repository;

import com.example.toychat.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // 사용자 이름으로 사용자 정보를 조회
    Optional<User> findByEmail(String email); // 이메일로 사용자 정보를 조회
}

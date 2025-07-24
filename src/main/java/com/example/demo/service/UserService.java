package com.example.demo.service;

import java.util.ArrayList;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.dto.PageResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.ArrayList;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 分页查询用户
     * @param page 页码(从0开始)
     * @param size 每页条数
     * @return 分页用户数据
     */
    public PageResponseDTO<User> getUsersByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);
        // 随机打乱当前页数据顺序
        List<User> content = new ArrayList<>(userPage.getContent());
        Collections.shuffle(content);
        return new PageResponseDTO<>(
            content,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages()
        );
    }

    public List<User> findUsersByAgeRangeAndUsername(Integer minAge, Integer maxAge, String keyword) {
        return userRepository.findByAgeRangeAndUsernameContaining(minAge, maxAge, keyword);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(userDetails.getUsername());
                    user.setAge(userDetails.getAge());
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
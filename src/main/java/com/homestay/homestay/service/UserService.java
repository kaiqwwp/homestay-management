package com.homestay.homestay.service;

import com.homestay.homestay.dto.UserDto;
import com.homestay.homestay.entity.User;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng với mã: " + id));
    }

    @Transactional
    public User create(UserDto dto) {
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new BusinessException("Tên đăng nhập '" + dto.getUsername() + "' đã tồn tại.");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new BusinessException("Mật khẩu không được để trống khi tạo mới.");
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        user.setFullName(dto.getFullName().trim());
        user.setRole(dto.getRole());
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UserDto dto) {
        User user = findById(id);

        if (!user.getUsername().equalsIgnoreCase(dto.getUsername().trim())) {
            if (userRepository.existsByUsername(dto.getUsername().trim())) {
                throw new BusinessException("Tên đăng nhập '" + dto.getUsername() + "' đã tồn tại.");
            }
            user.setUsername(dto.getUsername().trim());
        }

        user.setFullName(dto.getFullName().trim());
        user.setRole(dto.getRole());

        // Chỉ cập nhật mật khẩu nếu người dùng nhập mật khẩu mới
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException("Không thể xóa tài khoản quản trị mặc định 'admin'.");
        }
        userRepository.delete(user);
    }
}

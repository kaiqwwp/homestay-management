package com.homestay.homestay.service;

import com.homestay.homestay.dto.UserDto;
import com.homestay.homestay.entity.Customer;
import com.homestay.homestay.entity.User;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.CustomerRepository;
import com.homestay.homestay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private RealtimeEventService realtimeEventService;

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
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
        user.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setIdNumber(dto.getIdNumber() != null ? dto.getIdNumber().trim() : null);
        user.setRole(dto.getRole() != null && !dto.getRole().isBlank() ? dto.getRole() : "CUSTOMER");

        User saved = userRepository.save(user);

        // Nếu là CUSTOMER đăng ký mới, tự động tạo bản ghi trong bảng customers và đẩy thông báo real-time cho Admin/Nhân viên
        if ("CUSTOMER".equalsIgnoreCase(saved.getRole())) {
            Customer cust = syncOrCreateCustomerForUser(saved, null, null);
            if (cust != null && realtimeEventService != null) {
                String msg = String.format("Khách hàng mới đăng ký: %s - SĐT: %s (CCCD: %s)",
                        cust.getFullName(),
                        cust.getPhone() != null ? cust.getPhone() : "",
                        cust.getIdNumber() != null ? cust.getIdNumber() : "");
                realtimeEventService.publishCustomerEvent("CUSTOMER_REGISTERED", cust, msg);
            }
        }

        return saved;
    }

    @Transactional
    public User update(Long id, UserDto dto) {
        User user = findById(id);
        String oldEmail = user.getEmail();
        String oldPhone = user.getPhone();

        if (!user.getUsername().equalsIgnoreCase(dto.getUsername().trim())) {
            if (userRepository.existsByUsername(dto.getUsername().trim())) {
                throw new BusinessException("Tên đăng nhập '" + dto.getUsername() + "' đã tồn tại.");
            }
            user.setUsername(dto.getUsername().trim());
        }

        user.setFullName(dto.getFullName().trim());
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().trim());
        }
        if (dto.getIdNumber() != null) {
            user.setIdNumber(dto.getIdNumber().trim());
        }
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            user.setRole(dto.getRole());
        }

        // Chỉ cập nhật mật khẩu nếu người dùng nhập mật khẩu mới
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        }

        User saved = userRepository.save(user);
        if ("CUSTOMER".equalsIgnoreCase(saved.getRole())) {
            syncOrCreateCustomerForUser(saved, oldEmail, oldPhone);
        }
        return saved;
    }

    @Transactional
    public User updateOwnProfile(Long userId, String fullName, String phone, String idNumber, String email, String newPassword) {
        User user = findById(userId);
        String oldEmail = user.getEmail();
        String oldPhone = user.getPhone();

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Họ và tên không được để trống.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("Số điện thoại không được để trống.");
        }
        if (idNumber == null || idNumber.trim().isEmpty()) {
            throw new BusinessException("Số CCCD / Passport không được để trống.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email không được để trống.");
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone.trim());
        user.setIdNumber(idNumber.trim());
        user.setEmail(email.trim());

        if (newPassword != null && !newPassword.trim().isEmpty() && !"GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        User saved = userRepository.save(user);

        // Nếu là CUSTOMER (đặc biệt là khách đăng nhập Gmail vừa cập nhật CCCD & SĐT), tạo/đồng bộ ngay vào bảng customers
        if ("CUSTOMER".equalsIgnoreCase(saved.getRole())) {
            Customer cust = syncOrCreateCustomerForUser(saved, oldEmail, oldPhone);
            if (cust != null && realtimeEventService != null) {
                String msg = String.format("Khách hàng cập nhật hồ sơ: %s - SĐT: %s (CCCD: %s)",
                        cust.getFullName(), cust.getPhone(), cust.getIdNumber());
                realtimeEventService.publishCustomerEvent("CUSTOMER_UPDATED", cust, msg);
            }
        }

        return saved;
    }

    public Customer syncOrCreateCustomerForUser(User user, String oldEmail, String oldPhone) {
        if (user == null || !"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        // Chỉ đồng bộ sang danh sách Khách hàng khi đã có SĐT hoặc CCCD
        if ((user.getPhone() == null || user.getPhone().isBlank())
                && (user.getIdNumber() == null || user.getIdNumber().isBlank())) {
            return null;
        }

        Optional<Customer> existingCust = Optional.empty();
        if (oldEmail != null && !oldEmail.isBlank()) {
            existingCust = customerRepository.findFirstByEmailIgnoreCase(oldEmail);
        }
        if (existingCust.isEmpty() && user.getEmail() != null && !user.getEmail().isBlank()) {
            existingCust = customerRepository.findFirstByEmailIgnoreCase(user.getEmail());
        }
        if (existingCust.isEmpty() && oldPhone != null && !oldPhone.isBlank()) {
            existingCust = customerRepository.findFirstByPhone(oldPhone);
        }
        if (existingCust.isEmpty() && user.getPhone() != null && !user.getPhone().isBlank()) {
            existingCust = customerRepository.findFirstByPhone(user.getPhone());
        }

        Customer c = existingCust.orElseGet(Customer::new);
        c.setFullName(user.getFullName());
        c.setPhone(user.getPhone());
        c.setIdNumber(user.getIdNumber());
        c.setEmail(user.getEmail());
        return customerRepository.save(c);
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

package com.homestay.homestay.service;

import com.homestay.homestay.dto.CustomerDto;
import com.homestay.homestay.entity.Customer;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public CustomerService(CustomerRepository customerRepository, BookingRepository bookingRepository) {
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }

    public Page<Customer> searchCustomers(String keyword, Pageable pageable) {
        return customerRepository.searchCustomers(keyword, pageable);
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public List<Customer> searchByKeyword(String keyword) {
        return customerRepository.searchByKeyword(keyword);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với mã: " + id));
    }

    @Transactional
    public Customer create(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setFullName(dto.getFullName().trim());
        customer.setPhone(dto.getPhone().trim());
        customer.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        customer.setIdNumber(dto.getIdNumber() != null ? dto.getIdNumber().trim() : null);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerDto dto) {
        Customer customer = findById(id);
        customer.setFullName(dto.getFullName().trim());
        customer.setPhone(dto.getPhone().trim());
        customer.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        customer.setIdNumber(dto.getIdNumber() != null ? dto.getIdNumber().trim() : null);
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findById(id);
        long bookingCount = bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer().getId().equals(id))
                .count();
        if (bookingCount > 0) {
            throw new BusinessException("Không thể xóa khách hàng đã có lịch sử đặt phòng trong hệ thống.");
        }
        customerRepository.delete(customer);
    }
}

DROP DATABASE IF EXISTS homestay_db;
CREATE DATABASE homestay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE homestay_db;

-- 1. Bảng Tài khoản người dùng & Phân quyền (ADMIN, STAFF, CUSTOMER)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    id_number VARCHAR(30),                    -- CCCD / Hộ chiếu bắt buộc khi đặt phòng
    role VARCHAR(20) NOT NULL,                -- ADMIN, STAFF, CUSTOMER
    auth_provider VARCHAR(20) DEFAULT 'LOCAL' -- LOCAL, GOOGLE
);

-- 2. Bảng Hồ sơ Khách hàng (Tự động đồng bộ theo thời gian thực khi đăng ký / đặt phòng)
CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    id_number VARCHAR(30)                     -- CCCD / Hộ chiếu
);

-- 3. Bảng Loại phòng
CREATE TABLE room_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price_per_night DECIMAL(12,0) NOT NULL,
    capacity INT NOT NULL,
    description VARCHAR(500)
);

-- 4. Bảng Buồng phòng
CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE, OCCUPIED, MAINTENANCE
    image_url VARCHAR(255),
    FOREIGN KEY (room_type_id) REFERENCES room_types(id)
);

-- 5. Bảng Dịch vụ đi kèm
CREATE TABLE services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    unit_price DECIMAL(12,0) NOT NULL,
    unit VARCHAR(30)                          -- lần, suất, ngày, chai...
);

-- 6. Bảng Đơn đặt phòng & Lịch sử trạng thái chi tiết
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, BOOKED, REJECTED, CHECKED_IN, PAID, CHECKED_OUT, CANCELLED
    note VARCHAR(500),
    reject_reason VARCHAR(500),               -- Lý do từ chối đơn
    compensation_code VARCHAR(50),            -- Mã giảm giá đền bù 5-10% khi khách sạn từ chối đơn
    compensation_discount INT,                -- Phần trăm giảm giá đền bù (5 - 10)
    compensation_used BOOLEAN DEFAULT FALSE,  -- Trạng thái đã sử dụng mã đền bù
    applied_voucher_code VARCHAR(50),         -- Mã giảm giá khách áp dụng cho đơn này
    discount_percent INT DEFAULT 0,           -- Phần trăm được giảm trên đơn này
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME,                     -- Thời điểm khách sạn chấp nhận yêu cầu
    rejected_at DATETIME,                     -- Thời điểm khách sạn từ chối yêu cầu
    checked_in_at DATETIME,                   -- Thời điểm xác nhận khách nhận phòng (Mở khóa Thanh toán)
    paid_at DATETIME,                         -- Thời điểm thanh toán thành công
    checked_out_at DATETIME,                  -- Thời điểm hoàn tất lưu trú & trả phòng về Trống
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- 7. Bảng Chi tiết dịch vụ phát sinh theo đơn đặt phòng
CREATE TABLE booking_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,0) NOT NULL,        -- Lưu đơn giá tại thời điểm sử dụng
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

-- 8. Bảng Hóa đơn thanh toán (Hỗ trợ QR SePay MB Bank & Tiền mặt)
CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    room_total DECIMAL(12,0) NOT NULL,
    service_total DECIMAL(12,0) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,0) DEFAULT 0,  -- Số tiền giảm giá từ Voucher đền bù
    total DECIMAL(12,0) NOT NULL,
    paid_at DATETIME,
    payment_method VARCHAR(100),              -- QR SePay (MB Bank), Tiền mặt, Thẻ...
    payment_reference VARCHAR(100),           -- Mã giao dịch ngân hàng / SePay
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

DROP DATABASE IF EXISTS homestay_db;
CREATE DATABASE homestay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE homestay_db;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL,          -- ADMIN, STAFF, CUSTOMER
    auth_provider VARCHAR(20) DEFAULT 'LOCAL' -- LOCAL, GOOGLE
);

CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    id_number VARCHAR(30)              -- CCCD/passport
);

CREATE TABLE room_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price_per_night DECIMAL(12,0) NOT NULL,
    capacity INT NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE, OCCUPIED, MAINTENANCE
    image_url VARCHAR(255),
    FOREIGN KEY (room_type_id) REFERENCES room_types(id)
);

CREATE TABLE services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    unit_price DECIMAL(12,0) NOT NULL,
    unit VARCHAR(30)                   -- lần, suất, giờ...
);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',     -- BOOKED, CHECKED_IN, CHECKED_OUT, CANCELLED
    note VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE booking_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,0) NOT NULL,  -- lưu giá tại thời điểm dùng
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    room_total DECIMAL(12,0) NOT NULL,
    service_total DECIMAL(12,0) NOT NULL DEFAULT 0,
    total DECIMAL(12,0) NOT NULL,
    paid_at DATETIME,
    payment_method VARCHAR(30),
    payment_reference VARCHAR(100),
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

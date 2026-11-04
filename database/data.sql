USE homestay_db;

-- Users (Mật khẩu mặc định: admin123 cho admin/customer, staff123 cho staff)
INSERT INTO users (username, password, full_name, email, phone, id_number, role, auth_provider) VALUES
('admin', '$2a$10$LrKtRpIVlAQpCAuoXwlq9u1PeFUdLGFo/hCCRJQNIo3O65pSS64su', 'Quản trị viên', 'admin@homestay.com', '0909000001', '079000000001', 'ADMIN', 'LOCAL'),
('staff', '$2a$10$n/lF5XViaiPlAAxUvWLtqu7iOS7x7xuzoO3lYoJiMfK.fdH0XVn5W', 'Nhân viên Lễ tân', 'staff@homestay.com', '0909000002', '079000000002', 'STAFF', 'LOCAL'),
('customer', '$2a$10$LrKtRpIVlAQpCAuoXwlq9u1PeFUdLGFo/hCCRJQNIo3O65pSS64su', 'Khách hàng Thân thiết', 'customer@example.com', '0909000003', '079000000003', 'CUSTOMER', 'LOCAL');

INSERT INTO room_types (name, price_per_night, capacity, description) VALUES
('Phòng đơn', 300000, 1, 'Giường đơn, view vườn, máy lạnh, wifi tốc độ cao'),
('Phòng đôi', 500000, 2, 'Giường đôi King-size, ban công thoáng mát'),
('Phòng gia đình', 900000, 4, 'Hai giường đôi, bếp nhỏ, sofa và ban công');

-- 5 phòng trống (AVAILABLE) và 1 phòng bảo trì (MAINTENANCE) - Trạng thái mới vận hành
INSERT INTO rooms (room_number, room_type_id, status, image_url) VALUES
('101', 1, 'AVAILABLE', '/images/room-default.jpg'),
('102', 2, 'AVAILABLE', '/images/room-default.jpg'),
('201', 2, 'AVAILABLE', '/images/room-default.jpg'),
('202', 3, 'MAINTENANCE', '/images/room-default.jpg'),
('301', 1, 'AVAILABLE', '/images/room-default.jpg'),
('302', 3, 'AVAILABLE', '/images/room-default.jpg');

INSERT INTO services (name, unit_price, unit) VALUES
('Ăn sáng', 50000, 'suất'),
('Giặt ủi', 30000, 'lần'),
('Thuê xe máy', 150000, 'ngày'),
('Nước suối', 10000, 'chai'),
('Đưa đón sân bay', 250000, 'lượt');

-- Khách hàng (customers), Đơn đặt phòng (bookings) và Hóa đơn (invoices) để trống hoàn toàn, tự động sinh theo thời gian thực khi vận hành.

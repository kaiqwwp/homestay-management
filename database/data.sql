USE homestay_db;

-- Users (Mật khẩu mặc định: admin123 cho admin, staff123 cho staff)
-- DataInitializer của Spring Boot cũng sẽ tự động đồng bộ/tạo mới nếu chưa có
INSERT INTO users (username, password, full_name, role) VALUES
('admin', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxGQrvkWgyzG', 'Quản trị viên', 'ADMIN'),
('staff', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxGQrvkWgyzG', 'Nhân viên Lễ tân', 'STAFF');

INSERT INTO room_types (name, price_per_night, capacity, description) VALUES
('Phòng đơn', 300000, 1, 'Giường đơn, view vườn, máy lạnh, wifi tốc độ cao'),
('Phòng đôi', 500000, 2, 'Giường đôi King-size, ban công thoáng mát'),
('Phòng gia đình', 900000, 4, 'Hai giường đôi, bếp nhỏ, sofa và ban công');

INSERT INTO rooms (room_number, room_type_id, status, image_url) VALUES
('101', 1, 'AVAILABLE', '/images/room-default.jpg'),
('102', 2, 'OCCUPIED', '/images/room-default.jpg'),
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

INSERT INTO customers (full_name, phone, email, id_number) VALUES
('Nguyễn Văn A', '0900000001', 'a@example.com', '001200000001'),
('Trần Thị B', '0900000002', 'b@example.com', '001200000002'),
('Lê Hoàng C', '0900000003', 'c@example.com', '001200000003');

INSERT INTO bookings (customer_id, room_id, check_in, check_out, status, note) VALUES
(1, 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'CHECKED_IN', 'Khách yêu cầu phòng yên tĩnh'),
(2, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'BOOKED', 'Nhận phòng sau 14h'),
(3, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'CHECKED_OUT', 'Đã hoàn tất');

INSERT INTO booking_services (booking_id, service_id, quantity, unit_price) VALUES
(1, 1, 2, 50000),
(1, 4, 3, 10000),
(3, 2, 1, 30000);

INSERT INTO invoices (booking_id, room_total, service_total, total, paid_at, payment_method) VALUES
(3, 600000, 30000, 630000, NOW(), 'Tiền mặt');


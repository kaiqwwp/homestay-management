USE homestay_db;

INSERT INTO room_types (name, price_per_night, capacity, description) VALUES
('Phòng đơn', 300000, 1, 'Giường đơn, view vườn'),
('Phòng đôi', 500000, 2, 'Giường đôi, ban công'),
('Phòng gia đình', 900000, 4, 'Hai giường đôi, bếp nhỏ');

INSERT INTO rooms (room_number, room_type_id, status) VALUES
('101', 1, 'AVAILABLE'), ('102', 2, 'AVAILABLE'),
('201', 2, 'AVAILABLE'), ('202', 3, 'AVAILABLE');

INSERT INTO services (name, unit_price, unit) VALUES
('Ăn sáng', 50000, 'suất'), ('Giặt ủi', 30000, 'lần'), ('Thuê xe máy', 150000, 'ngày');

INSERT INTO customers (full_name, phone, email) VALUES
('Nguyễn Văn A', '0900000001', 'a@example.com');

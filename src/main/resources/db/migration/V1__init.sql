-- Crear la tabla de salas
CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Crear la tabla de usuarios
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    dni VARCHAR(15) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    room_id BIGINT NULL,
    CONSTRAINT fk_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insertar datos iniciales
INSERT INTO rooms (name) VALUES ('Sala 1'), ('Sala 2');

INSERT INTO users (name, email, dni, phone, role, room_id) VALUES
('Juan', 'juan@example.com', '11111111A', '600000001', 'admin', 1),
('Maria', 'maria@example.com', '22222222B', '600000002', 'superadmin', 2);

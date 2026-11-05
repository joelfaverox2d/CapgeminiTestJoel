-- Salas de prueba: la 1 es la sala por defecto, la 2 sirve para probar el cambio de sala
INSERT INTO rooms (id, name) VALUES (1, 'Sala 1');
INSERT INTO rooms (id, name) VALUES (2, 'Sala 2');
-- Evita que los ids autogenerados choquen con los ids insertados a mano
ALTER TABLE rooms ALTER COLUMN id RESTART WITH 100;

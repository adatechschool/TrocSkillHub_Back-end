-- V3__add_ada_lovelace_user.sql

INSERT INTO "users" (first_name, last_name, picture, email, password, address, city, country, phone_number, description, created_at, updated_at)
VALUES 
('Ada', 'Lovelace', NULL, 'ada.lovelace@example.fr', '$2a$10$yAB8k5LKAnY6I9ZwO3M4QfS2uB0cD4eF6gH8iJ0kL2mN4oP6qR8sT', '15 rue des Mathématiques', 'Paris', 'France', '+33 6 12 34 56 78', 'Développeuse passionnée par les algorithmes et l''informatique. Pionnière de la programmation.', '2024-01-18', NULL);
--V2__seed_add_data_users.sql

INSERT INTO "user" (first_name, last_name, picture, email, password, address, city, country, phone_number, description, created_at, updated_at)
VALUES 

('Jean', 'Martin', NULL, 'jean.martin@example.fr', '$2a$10$yAB8k5LKAnY6I9ZwO3M4QfS2uB0cD4eF6gH8iJ0kL2mN4oP6qR8sT', '42 avenue des Champs', 'Lyon', 'France', '+33 6 23 45 67 89', 'Graphiste freelance, jaime créer des designs modernes et épurés.', '2024-01-16', NULL),

('Sophie', 'Lefebvre', NULL, 'sophie.lefebvre@example.fr', '$2a$10$zBC9l6MLBoZ7J0AxP4N5RgT3vC1dE5fG7hI9jK1lM3nO5pQ7rS9uU', '8 boulevard Victor Hugo', 'Paris', 'France', '+33 6 34 56 78 90', 'Professeur de yoga et passionnée de bien-être. J''adore partager mes connaissances.', '2024-01-17', NULL);
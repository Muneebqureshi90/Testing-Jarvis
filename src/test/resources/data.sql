-- Test data for integration tests
INSERT INTO users (id, uuid, username, email, password_hash, first_name, last_name, phone, is_active, is_verified, created_at, updated_at) VALUES
(1, '11111111-1111-1111-1111-111111111111', 'testuser', 'test@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO', 'Test', 'User', '+1234567890', TRUE, FALSE, NOW(), NOW()),
(2, '22222222-2222-2222-2222-222222222222', 'adminuser', 'admin@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO', 'Admin', 'User', '+0987654321', TRUE, TRUE, NOW(), NOW()),
(3, '33333333-3333-3333-3333-333333333333', 'inactiveuser', 'inactive@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj12NHe1zhCO', 'Inactive', 'User', NULL, FALSE, FALSE, NOW(), NOW());

INSERT INTO posts (id, title, content, author_id, published, created_at, updated_at) VALUES
(1, 'First Test Post', 'Content for first post', 1, TRUE, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(2, 'Second Test Post', 'Content for second post', 1, FALSE, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
(3, 'Admin Post', 'Content by admin', 2, TRUE, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY);

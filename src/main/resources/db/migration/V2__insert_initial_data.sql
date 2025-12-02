-- ===============================
-- V2: INSERT INITIAL DATA
-- ===============================

-- Insert roles
INSERT INTO roles(name) VALUES ('ADMIN');
INSERT INTO roles(name) VALUES ('USER');
INSERT INTO roles(name) VALUES ('MODERATOR');

-- 50 profiles (H2 function system_range creates a sequence)
INSERT INTO profiles(first_name, last_name)
SELECT 'FirstName' || x, 'LastName' || x
FROM system_range(1, 50);

-- 50 users with OneToOne mapping to profiles
INSERT INTO users(username, email, profile_id)
SELECT 'user' || x,
       'user' || x || '@example.com',
       x
FROM system_range(1, 50);

-- Assign each user the USER role (role_id = 2)
INSERT INTO user_roles(user_id, role_id)
SELECT id, 2
FROM users;

-- First batch of posts (30 posts)
INSERT INTO posts(user_id, title, content)
SELECT id,
       'Post of ' || username,
       'Content for ' || username
FROM users
WHERE id <= 30;

-- Second batch of posts (extra 70 posts → total 100+)
INSERT INTO posts(user_id, title, content)
SELECT id,
       'Second post of ' || username,
       'Another content for ' || username
FROM users
WHERE id <= 35;
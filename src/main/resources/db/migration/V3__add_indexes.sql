-- Indexes for performance optimization

-- Users
CREATE INDEX IF NOT EXISTS idx_users_username
    ON users(username);

CREATE INDEX IF NOT EXISTS idx_users_email
    ON users(email);

-- Posts (One-To-Many with users)
CREATE INDEX IF NOT EXISTS idx_posts_user_id
    ON posts(user_id);

-- User roles (Many-To-Many)
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id
    ON user_roles(user_id);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id
    ON user_roles(role_id);
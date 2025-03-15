-- Create users table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    provider VARCHAR(50) DEFAULT 'local',
    address VARCHAR(255),
    gender VARCHAR(20),
    age INT,
    profile_photo LONGBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Create otp_verifications table
CREATE TABLE IF NOT EXISTS otp_verifications (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expiration_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     email VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expiration_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (email)
    );

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_otp_verifications_email ON otp_verifications(email);
CREATE INDEX idx_password_reset_tokens_email ON password_reset_tokens(email);
-- Database setup script for Personal Finance Tracker Microservices

-- Create databases for each microservice
CREATE DATABASE IF NOT EXISTS fintrack_auth;
CREATE DATABASE IF NOT EXISTS fintrack_user;
CREATE DATABASE IF NOT EXISTS fintrack_expense;

-- Create a dedicated user for the application
CREATE USER IF NOT EXISTS 'fintrack_app'@'localhost' IDENTIFIED BY 'fintrack_password';

-- Grant privileges to the application user
GRANT ALL PRIVILEGES ON fintrack_auth.* TO 'fintrack_app'@'localhost';
GRANT ALL PRIVILEGES ON fintrack_user.* TO 'fintrack_app'@'localhost';
GRANT ALL PRIVILEGES ON fintrack_expense.* TO 'fintrack_app'@'localhost';

FLUSH PRIVILEGES;
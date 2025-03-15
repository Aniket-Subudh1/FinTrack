-- Create expenses table
CREATE TABLE IF NOT EXISTS expenses (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        amount DOUBLE NOT NULL,
                                        category VARCHAR(50) NOT NULL,
    date DATETIME NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Create indexes for performance
CREATE INDEX idx_expenses_user_email ON expenses(user_email);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_date ON expenses(date);
CREATE INDEX idx_expenses_user_category ON expenses(user_email, category);
CREATE INDEX idx_expenses_user_date ON expenses(user_email, date);
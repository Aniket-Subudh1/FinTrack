package com.personalfinance.expense.exception;

public class ExpenseException extends RuntimeException {

    public ExpenseException(String message) {
        super(message);
    }

    public ExpenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
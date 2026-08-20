package com.ecommerce.auth.exception;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(String status) {
        super("Account is not active (status=" + status + ")");
    }
}

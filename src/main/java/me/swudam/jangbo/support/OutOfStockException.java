package me.swudam.jangbo.support;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }

}

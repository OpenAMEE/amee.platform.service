package com.amee.calculation.service;

/**
 * A RuntimeException for capturing exceptions arising from algorithm calculations.
 */
public class CalculationException extends RuntimeException {

    private int errorCode = -1;

    public CalculationException(String message) {
        super(message);
    }

    public CalculationException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getError() {
        return errorCode + ":" + getMessage();
    }
}
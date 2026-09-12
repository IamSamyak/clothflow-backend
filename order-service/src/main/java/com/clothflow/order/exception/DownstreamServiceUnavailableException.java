package com.clothflow.order.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    public DownstreamServiceUnavailableException(
            String serviceName
    ) {
        super(
                serviceName +
                        " is temporarily unavailable"
        );
    }
}
package com.nicolebertolo.mscustomer.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerNotFoundException extends RuntimeException{

    private String message;
}

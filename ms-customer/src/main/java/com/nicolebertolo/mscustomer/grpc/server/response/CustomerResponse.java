package com.nicolebertolo.mscustomer.grpc.server.response;

import com.nicolebertolo.mscustomer.domain.models.Address;
import com.nicolebertolo.mscustomer.domain.models.CustomerDocument;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerResponse {
    private String id;
    private String name;
    private String lastname;
    private List<CustomerDocument> documents;
    private List<Address> addresses;
}

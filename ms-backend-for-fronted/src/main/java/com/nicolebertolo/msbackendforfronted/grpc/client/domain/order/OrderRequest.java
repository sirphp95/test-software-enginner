package com.nicolebertolo.msbackendforfronted.grpc.client.domain.order;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderRequest {
    private String customerId;
    private List<String> productsIds;
}

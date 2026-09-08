package com.suraj.product_service.service;

import com.suraj.product_service.dto.ProductRequest;
import com.suraj.product_service.model.Product;
import com.suraj.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public void createRequest(ProductRequest productRequest){
        Product product = Product.builder()
                        .name(productRequest.getName())
                        .description(productRequest.getDescription())
                        .price(productRequest.getPrice())
                        .build();

        productRepository.save(product);
        log.info("Product is saved with id:[{}]", product.getId());
    }

}

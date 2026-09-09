package com.suraj.product_service.service;

import com.suraj.product_service.dto.ProductRequest;
import com.suraj.product_service.model.Product;
import com.suraj.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository repository;
    @InjectMocks
    private ProductService service;

    @Test
    void shouldMapRequestAndSaveProduct() {
        var request = new ProductRequest("Phone", "Description", new BigDecimal("1200.50"));

        service.createRequest(request);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getName()).isEqualTo(request.getName());
        assertThat(saved.getDescription()).isEqualTo(request.getDescription());
        assertThat(saved.getPrice()).isEqualByComparingTo(request.getPrice());
    }

    @Test
    void shouldMapEveryProductToResponse() {
        var first = new Product("id-1", "Phone", "First", new BigDecimal("1200.50"));
        var second = new Product("id-2", "Laptop", "Second", new BigDecimal("2000.75"));
        when(repository.findAll()).thenReturn(List.of(first, second));

        var responses = service.getAllProducts();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0)).usingRecursiveComparison().isEqualTo(first);
        assertThat(responses.get(1)).usingRecursiveComparison().isEqualTo(second);
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.getAllProducts()).isEmpty();
    }

    @Test
    void shouldPropagateSaveFailure() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(repository.save(any(Product.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.createRequest(
                new ProductRequest("Phone", "Description", BigDecimal.TEN)))
                .isSameAs(failure);
    }

    @Test
    void shouldPropagateReadFailure() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(repository.findAll()).thenThrow(failure);
        assertThatThrownBy(service::getAllProducts).isSameAs(failure);
    }
}

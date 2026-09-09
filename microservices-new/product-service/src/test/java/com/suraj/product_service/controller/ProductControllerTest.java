package com.suraj.product_service.controller;

import com.suraj.product_service.dto.ProductRequest;
import com.suraj.product_service.dto.ProductResponse;
import com.suraj.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProductService service;

    @Test
    void shouldDeserializeRequestAndReturnCreated() throws Exception {
        mockMvc.perform(post("/api/product").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Phone","description":"Description","price":1200.50}
                                """))
                .andExpect(status().isCreated());
        verify(service).createRequest(new ProductRequest("Phone", "Description", new BigDecimal("1200.50")));
    }

    @Test
    void shouldSerializeAllProducts() throws Exception {
        when(service.getAllProducts()).thenReturn(List.of(
                new ProductResponse("id-1", "Phone", "First", new BigDecimal("1200.50")),
                new ProductResponse("id-2", "Laptop", "Second", new BigDecimal("2000.75"))));
        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [{"id":"id-1","name":"Phone","description":"First","price":1200.50},
                         {"id":"id-2","name":"Laptop","description":"Second","price":2000.75}]
                        """));
    }

    @Test
    void shouldReturnEmptyJsonArray() throws Exception {
        when(service.getAllProducts()).thenReturn(List.of());
        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/product").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectMissingBody() throws Exception {
        mockMvc.perform(post("/api/product").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectNonNumericPrice() throws Exception {
        mockMvc.perform(post("/api/product").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Phone","price":"invalid"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/product").contentType(MediaType.TEXT_PLAIN).content("Phone"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }
}

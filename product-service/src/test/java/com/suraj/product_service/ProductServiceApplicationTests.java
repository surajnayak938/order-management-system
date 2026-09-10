package com.suraj.product_service;

import com.suraj.product_service.dto.ProductRequest;
import com.suraj.product_service.model.Product;
import com.suraj.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
@Testcontainers
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private ProductRepository productRepository;

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0")
			// Work around MongoDB SERVER-121912 on Docker kernels 6.19 and newer.
			.withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");

	@Test
	void shouldCreateProduct() throws Exception {
		ProductRequest productRequest = getProductRequest();
		String productMappedToString = objectMapper.writeValueAsString(productRequest);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
				.contentType(MediaType.APPLICATION_JSON)
				.content(productMappedToString))
				.andExpect(status().isCreated());
		assertThat(productRepository.findAll()).singleElement().satisfies(product -> {
			assertThat(product.getId()).isNotBlank();
			assertThat(product.getName()).isEqualTo(productRequest.getName());
			assertThat(product.getDescription()).isEqualTo(productRequest.getDescription());
			assertThat(product.getPrice()).isEqualByComparingTo(productRequest.getPrice());
		});
	}

	@Test
	void shouldGetAllProducts() throws Exception {
		Product product = productRepository.save(
				Product.builder().name("Iphone").description("Apple 17 pro max").price(BigDecimal.valueOf(1200)).build()
		);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/product")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(product.getId()))
				.andExpect(jsonPath("$[0].name").value("Iphone"))
				.andExpect(jsonPath("$[0].description")
						.value("Apple 17 pro max"))
				.andExpect(jsonPath("$[0].price").value(1200));

	}

	@BeforeEach
	void clearTestDatabase() {
		productRepository.deleteAll();
	}

	@Test
	void shouldGetMultipleProducts() throws Exception {
		Product first = productRepository.save(new Product(null, "Phone", "First", new BigDecimal("10.50")));
		Product second = productRepository.save(new Product(null, "Laptop", "Second", new BigDecimal("20.75")));

		String json = mockMvc.perform(MockMvcRequestBuilders.get("/api/product"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		var responses = objectMapper.readValue(json, com.suraj.product_service.dto.ProductResponse[].class);
		assertThat(responses).extracting(com.suraj.product_service.dto.ProductResponse::getId)
				.containsExactlyInAnyOrder(first.getId(), second.getId());
	}

	@Test
	void shouldGetEmptyListFromEmptyDatabase() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/product"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldPersistAndReadProductById() {
		Product saved = productRepository.save(new Product(null, "Phone", "Description", new BigDecimal("19.99")));
		assertThat(saved.getId()).isNotBlank();
		assertThat(productRepository.findById(saved.getId())).hasValueSatisfying(loaded -> {
			assertThat(loaded.getName()).isEqualTo("Phone");
			assertThat(loaded.getDescription()).isEqualTo("Description");
			assertThat(loaded.getPrice()).isEqualByComparingTo("19.99");
		});
	}

	private ProductRequest getProductRequest(){
		return ProductRequest.builder().name("Iphone").description("Apple 17 pro max").price(BigDecimal.valueOf(1200)).build();
	}

}

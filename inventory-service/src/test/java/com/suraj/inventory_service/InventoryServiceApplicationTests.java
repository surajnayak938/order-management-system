package com.suraj.inventory_service;

import com.suraj.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryServiceApplicationTests {

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private CommandLineRunner loadData;

	@Test
	void startupLoadsBothProductsWithTheirExpectedQuantities() {
		assertThat(inventoryRepository.findAll())
				.extracting("skuCode", "quantity")
				.containsExactlyInAnyOrder(
						tuple("iphone_13", 100),
						tuple("iPhone_13_red", 50));
	}

	@Test
	void runningLoaderAgainDoesNotDuplicateProductsOrResetStock() throws Exception {
		var inventory = inventoryRepository.findBySkuCode("iphone_13").orElseThrow();
		inventory.setQuantity(42);
		inventoryRepository.saveAndFlush(inventory);

		loadData.run();
		loadData.run();

		assertThat(inventoryRepository.findAll())
				.extracting("skuCode", "quantity")
				.containsExactlyInAnyOrder(
						tuple("iphone_13", 42),
						tuple("iPhone_13_red", 50));
	}

	@Test
	void loaderAddsMissingProductWhenAnotherProductAlreadyExists() throws Exception {
		var redPhone = inventoryRepository.findBySkuCode("iPhone_13_red").orElseThrow();
		inventoryRepository.delete(redPhone);
		inventoryRepository.flush();

		loadData.run();

		assertThat(inventoryRepository.findAll())
				.extracting("skuCode", "quantity")
				.containsExactlyInAnyOrder(
						tuple("iphone_13", 100),
						tuple("iPhone_13_red", 50));
	}

}

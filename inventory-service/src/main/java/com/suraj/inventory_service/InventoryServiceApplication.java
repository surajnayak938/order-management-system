package com.suraj.inventory_service;

import com.suraj.inventory_service.model.Inventory;
import com.suraj.inventory_service.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository){
		return args -> {
			seedInventoryIfMissing(inventoryRepository, "iphone_13", 100);
			seedInventoryIfMissing(inventoryRepository, "iPhone_13_red", 50);
		};
	}

	private void seedInventoryIfMissing(InventoryRepository repository, String skuCode, int quantity) {
		if (!repository.existsBySkuCode(skuCode)) {
			Inventory inventory = new Inventory();
			inventory.setSkuCode(skuCode);
			inventory.setQuantity(quantity);
			repository.save(inventory);
		}
	}
}

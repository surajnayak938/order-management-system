package com.suraj.inventory_service.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryResponseDTOTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void decodesInventoryResponseWithBothStockStates() {
        InventoryResponseDTO[] response = mapper.readValue("""
                [{"skuCode":"iphone_13","inStock":true},
                 {"skuCode":"iPhone_13_red","inStock":false}]
                """, InventoryResponseDTO[].class);
        assertThat(response[0].isInStock()).isTrue();
        assertThat(response[1].isInStock()).isFalse();
    }

    @Test
    void serializesStockFlagWithTheSameNameUsedForDecoding() {
        for (boolean stock : new boolean[]{true, false}) {
            String json = mapper.writeValueAsString(new InventoryResponseDTO("phone", stock, stock ? 100 : 0));
            assertThat(mapper.readTree(json).get("inStock").asBoolean()).isEqualTo(stock);
            assertThat(mapper.readTree(json).has("isInStock")).isFalse();
            assertThat(mapper.readValue(json, InventoryResponseDTO.class).isInStock()).isEqualTo(stock);
        }
    }
}

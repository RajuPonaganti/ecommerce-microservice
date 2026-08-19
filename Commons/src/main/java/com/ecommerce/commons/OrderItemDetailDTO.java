package com.ecommerce.commons;
/*
 * package com.ecommerce.order.dtos;
 * 
 * import java.math.BigDecimal; import java.util.UUID;
 * 
 * import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
 * 
 * @Setter
 * 
 * @Getter
 * 
 * @NoArgsConstructor public class OrderItemDetailDTO { private UUID productId;
 * 
 * // (unique per seller) private String sku;
 * 
 * private int quantity;
 * 
 * private BigDecimal unitPrice;
 * 
 * private UUID sellerId; }
 */

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDetailDTO(UUID productId, String sku, int quantity, BigDecimal unitPrice,	UUID sellerId) {}

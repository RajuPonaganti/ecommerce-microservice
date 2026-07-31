package com.ecommerce.product.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(columnNames = { "seller_id", "sku" }))
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID productId;

	// @Column specifies the database column properties
	@Column(nullable = false, length = 500) // nullable=false means NOT NULL in SQL
	private String title;

	// @Column specifies the database column properties
	@Lob
	private String description;

	// @Column specifies the database column properties
	@Column(nullable = false, length = 100) // nullable=false means NOT NULL in SQL
	private String brand;

	@Column(nullable = false, precision = 12, scale = 2) // e.g., 999999999.99
	private BigDecimal price;

	@Column(precision = 12, scale = 2) // e.g., 999999999.99
	private BigDecimal mrp;

	@Column(nullable = false)
	private String currency;

	@Column(nullable = false)
	private String category;

	// @Enumerated stores enum as  a string in the DB (not a number)
	// Using STRING is safer — if you reorder enum values, DB is not affected
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductStatus status = ProductStatus.DRAFT;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> attributes;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(nullable = false, length = 100)
	private String sku;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "product")
	private List<ProductImages> productImages;

}

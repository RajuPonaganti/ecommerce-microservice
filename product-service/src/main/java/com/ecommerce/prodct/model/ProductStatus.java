package com.ecommerce.prodct.model;

public enum ProductStatus {
	DRAFT, // seller just created it, not visible yet
	PENDING_REVIEW, // submitted for approval
	ACTIVE, // visible to customers
	DISCONTINUED // no longer for sale
}

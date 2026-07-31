package com.ecommerce.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountDetailsDTO {
	private String bankAccountId;
	@NotNull
	private long accountNumber;
	@NotBlank
	@Size(max = 15)
	private String IFSC;

	@NotBlank
	@Size(max = 100)
	private String accountHolderName;

	@NotBlank
	@Size(max = 20)
	private String accountType;
}

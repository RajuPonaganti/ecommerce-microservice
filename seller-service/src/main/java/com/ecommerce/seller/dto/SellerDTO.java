package com.ecommerce.seller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class SellerDTO {

	private UUID sellerId;

	@NotBlank
	@Size(max = 250)
	private String legalName;

	@NotBlank
	@Size(max = 100)
	private String tradeName;

	@NotBlank
	@Size(max = 20)
	private String gstin;

	@NotBlank
	@Size(max = 10)
	private String pan;

	@Valid
	private BankAccountDetailsDTO baDetails;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SellerDTO [sellerId=");
		builder.append(sellerId);
		builder.append(", legalName=");
		builder.append(legalName);
		builder.append(", tradeName=");
		builder.append(tradeName);
		builder.append(", gstin=");
		builder.append(gstin);
		builder.append(", pan=");
		builder.append(pan);
		builder.append(", baDetails=");
		builder.append(baDetails);
		builder.append("]");
		return builder.toString();
	}

}

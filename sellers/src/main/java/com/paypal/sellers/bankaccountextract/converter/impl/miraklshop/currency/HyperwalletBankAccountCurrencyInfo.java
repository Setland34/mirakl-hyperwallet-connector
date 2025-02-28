package com.paypal.sellers.bankaccountextract.converter.impl.miraklshop.currency;

import com.paypal.sellers.bankaccountextract.model.TransferType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class HyperwalletBankAccountCurrencyInfo {

	private String country;

	private String currency;

	private TransferType transferType;

}

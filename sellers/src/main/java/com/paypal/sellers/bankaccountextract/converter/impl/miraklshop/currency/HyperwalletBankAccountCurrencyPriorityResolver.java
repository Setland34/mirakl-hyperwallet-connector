package com.paypal.sellers.bankaccountextract.converter.impl.miraklshop.currency;

import com.paypal.sellers.bankaccountextract.model.TransferType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HyperwalletBankAccountCurrencyPriorityResolver {

	private final HyperwalletBankAccountCurrencyResolutionConfiguration hyperwalletBankAccountCurrencyResolutionConfiguration;

	private List<TransferType> transferTypePriority = List.of(TransferType.BANK_ACCOUNT, TransferType.WIRE_ACCOUNT);

	public HyperwalletBankAccountCurrencyPriorityResolver(
			HyperwalletBankAccountCurrencyResolutionConfiguration hyperwalletBankAccountCurrencyResolutionConfiguration) {
		this.hyperwalletBankAccountCurrencyResolutionConfiguration = hyperwalletBankAccountCurrencyResolutionConfiguration;
	}

	public List<HyperwalletBankAccountCurrencyInfo> sortCurrenciesByPriority(
			List<HyperwalletBankAccountCurrencyInfo> currencyCandidates) {
		if (CollectionUtils.isEmpty(currencyCandidates)) {
			return Collections.emptyList();
		}

		List<HyperwalletBankAccountCurrencyInfo> sortedCurrencyCandidates = new ArrayList<>(currencyCandidates);

		if (hyperwalletBankAccountCurrencyResolutionConfiguration.isOverrideCurrencySelectionPriority()) {
			Collections.sort(sortedCurrencyCandidates,
					Comparator.comparing(HyperwalletBankAccountCurrencyInfo::getCountry)
							.thenComparing(this::compareCurrencyInfoPriority));
		}
		else if (hyperwalletBankAccountCurrencyResolutionConfiguration.isPrioritizeBankAccountTypeOverCurrency()) {
			Collections.sort(sortedCurrencyCandidates,
					Comparator.comparing(HyperwalletBankAccountCurrencyInfo::getCountry)
							.thenComparing(this::compareTransferTypePriority));
		}
		else {
			Collections.sort(sortedCurrencyCandidates,
					Comparator.comparing(HyperwalletBankAccountCurrencyInfo::getCountry)
							.thenComparing(this::compareTransferTypeOnlyForSameCurrencies));
		}

		return sortedCurrencyCandidates;
	}

	private int compareCurrencyInfoPriority(HyperwalletBankAccountCurrencyInfo c1,
			HyperwalletBankAccountCurrencyInfo c2) {
		// The compare algorithm is as follows:
		// 1. If prioritizeBankAccountTypeOverCurrency is enabled, then:
		// 1.1. we sort the currencies by transfer type, being BANK_ACCOUNT the one with
		// the highest priority.
		// 1.2. If the currencies have the same transfer type, then we sort them by
		// priority.
		// 2. If prioritizeBankAccountTypeOverCurrency is disabled, then:
		// 2.1 we sort the currencies by priority.
		// 2.2 If the currencies have the same priority, then we sort them by transfer
		// type, being BANK_ACCUNT the one with the
		// highest priority
		if (hyperwalletBankAccountCurrencyResolutionConfiguration.isPrioritizeBankAccountTypeOverCurrency()) {
			if (compareTransferTypePriority(c1, c2) == 0) {
				return compareCurrencyPriority(c1, c2);
			}
			else {
				return compareTransferTypePriority(c1, c2);
			}
		}
		else {
			if (compareCurrencyPriority(c1, c2) == 0) {
				return compareTransferTypePriority(c1, c2);
			}
			else {
				return compareCurrencyPriority(c1, c2);
			}
		}
	}

	private int compareTransferTypeOnlyForSameCurrencies(HyperwalletBankAccountCurrencyInfo c1,
			HyperwalletBankAccountCurrencyInfo c2) {
		if (c1.getCurrency().equals(c2.getCurrency())) {
			return compareTransferTypePriority(c1, c2);
		}
		else {
			return 0;
		}
	}

	private int compareCurrencyPriority(HyperwalletBankAccountCurrencyInfo c1, HyperwalletBankAccountCurrencyInfo c2) {
		return Integer.compare(getCurrencyPriority(c1), getCurrencyPriority(c2));
	}

	private int getCurrencyPriority(HyperwalletBankAccountCurrencyInfo currencyInfo) {
		// The priority of a currency is defined by the index of the currency in the list
		// of currencies.
		// The currency with the highest priority is the one with the lowest index.
		// Per country priority has a higher priority than the global priority.
		// If the currency is not found in the list, then the priority is -1
		List<String> currencyPriorityPerCountry = hyperwalletBankAccountCurrencyResolutionConfiguration
				.getPerCountryCurrencyPriority().get(currencyInfo.getCountry());
		List<String> globalCurrencyPriority = hyperwalletBankAccountCurrencyResolutionConfiguration
				.getGlobalCurrencyPriority();
		String currency = currencyInfo.getCurrency();
		if (CollectionUtils.isNotEmpty(currencyPriorityPerCountry) && currencyPriorityPerCountry.contains(currency)) {
			return currencyPriorityPerCountry.indexOf(currency);
		}
		else if (CollectionUtils.isNotEmpty(globalCurrencyPriority) && globalCurrencyPriority.contains(currency)) {
			return globalCurrencyPriority.indexOf(currency);
		}

		return Integer.MAX_VALUE;
	}

	private int compareTransferTypePriority(HyperwalletBankAccountCurrencyInfo c1,
			HyperwalletBankAccountCurrencyInfo c2) {
		// Currency Info with the higher transfer type priority should be first in the
		// resulting list
		return Integer.compare(getTransferTypePriority(c1), getTransferTypePriority(c2));
	}

	private int getTransferTypePriority(HyperwalletBankAccountCurrencyInfo currencyInfo) {
		return transferTypePriority.indexOf(currencyInfo.getTransferType());
	}

}

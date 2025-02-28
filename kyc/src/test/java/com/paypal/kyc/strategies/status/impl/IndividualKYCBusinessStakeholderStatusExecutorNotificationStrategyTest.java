package com.paypal.kyc.strategies.status.impl;

import com.hyperwallet.clientsdk.Hyperwallet;
import com.hyperwallet.clientsdk.HyperwalletException;
import com.hyperwallet.clientsdk.model.HyperwalletUser;
import com.mirakl.client.core.exception.MiraklException;
import com.mirakl.client.mmp.operator.domain.shop.update.MiraklUpdatedShops;
import com.mirakl.client.mmp.operator.request.shop.MiraklUpdateShopsRequest;
import com.mirakl.client.mmp.request.additionalfield.MiraklRequestAdditionalFieldValue;
import com.paypal.infrastructure.exceptions.HMCException;
import com.paypal.infrastructure.hyperwallet.api.HyperwalletSDKUserService;
import com.paypal.infrastructure.hyperwallet.api.UserHyperwalletApiConfig;
import com.paypal.infrastructure.sdk.mirakl.MiraklMarketplacePlatformOperatorApiWrapper;
import com.paypal.kyc.model.KYCBusinessStakeholderStatusNotificationBodyModel;
import com.paypal.kyc.model.KYCConstants;
import com.paypal.kyc.service.documents.files.mirakl.MiraklBusinessStakeholderDocumentsExtractService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndividualKYCBusinessStakeholderStatusExecutorNotificationStrategyTest {

	private static final String CLIENT_ID = "0229";

	private static final String UK_TOKEN = "ukToken";

	private static final String MIRAKL_SHOP_ID = "0229";

	private static final String USER_TOKEN = "userToken";

	private static final String BSTK_TOKEN = "bstkToken";

	private static final String DEFAULT_TOKEN = "defaultToken";

	private static final String CUSTOM_FIELD_1 = "customField1";

	private static final String CUSTOM_FIELD_2 = "customField2";

	private static final String BSTK_PROOF_IDENTITY_FIELD = "bstkProofIdentityField";

	private static final Map<String, String> USER_STORE_TOKENS = Map.of("DEFAULT", DEFAULT_TOKEN, "UK", UK_TOKEN);

	private static final String HMC_EXCEPTION_MESSAGE = "No Hyperwallet users were found for user token %s in the system instance(s)";

	@InjectMocks
	private IndividualKYCBusinessStakeholderStatusNotificationStrategy testObj;

	@Mock
	private HyperwalletSDKUserService hyperwalletSDKUserService;

	@Mock
	private UserHyperwalletApiConfig kycHyperwalletApiConfigMock;

	@Mock
	private MiraklMarketplacePlatformOperatorApiWrapper miraklMarketplacePlatformOperatorApiClientMock;

	@Mock
	private MiraklBusinessStakeholderDocumentsExtractService miraklBusinessStakeholderDocumentsExtractServiceMock;

	@Mock
	private Hyperwallet hyperwalletMock;

	@Mock
	private HyperwalletUser hyperwalletUserMock;

	@Mock
	private MiraklUpdatedShops miraklUpdatedShopsMock;

	@Mock
	private KYCBusinessStakeholderStatusNotificationBodyModel kycBusinessStakeholderStatusNotificationBodyModelMock;

	@Captor
	private ArgumentCaptor<MiraklUpdateShopsRequest> miraklUpdateShopsRequestArgumentCaptor;

	@Test
	void isApplicable_whenBusinessStakeholderTypeIsNull_shouldReturnFalse() {
		final boolean result = testObj.isApplicable(kycBusinessStakeholderStatusNotificationBodyModelMock);

		assertThat(result).isFalse();
	}

	@Test
	void isApplicable_whenBusinessStakeholderTypeIsNotIndividual_shouldReturnFalse() {
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getProfileType())
				.thenReturn(HyperwalletUser.ProfileType.BUSINESS);

		final boolean result = testObj.isApplicable(kycBusinessStakeholderStatusNotificationBodyModelMock);

		assertThat(result).isFalse();
	}

	@Test
	void isApplicable_whenBusinessStakeholderTypeIsIndividualAndNotificationTypeIsVerificationStatus_shouldReturnTrue() {
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getProfileType())
				.thenReturn(HyperwalletUser.ProfileType.INDIVIDUAL);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getHyperwalletWebhookNotificationType()).thenReturn(
				KYCConstants.HwWebhookNotificationType.USERS_BUSINESS_STAKEHOLDERS_VERIFICATION_STATUS + ".REQUIRED");

		final boolean result = testObj.isApplicable(kycBusinessStakeholderStatusNotificationBodyModelMock);

		assertThat(result).isTrue();
	}

	@Test
	void isApplicable_whenBusinessStakeholderTypeIsIndividualAndNotificationTypeIsNotVerificationStatus_shouldReturnTrue() {
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getProfileType())
				.thenReturn(HyperwalletUser.ProfileType.INDIVIDUAL);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getHyperwalletWebhookNotificationType())
				.thenReturn(KYCConstants.HwWebhookNotificationType.USERS_BUSINESS_STAKEHOLDERS_CREATED);

		final boolean result = testObj.isApplicable(kycBusinessStakeholderStatusNotificationBodyModelMock);

		assertThat(result).isFalse();
	}

	@Test
	void callHyperwalletSDKCatchingException_shouldReturnHyperwalletUser() {
		when(hyperwalletMock.getUser(USER_TOKEN)).thenReturn(hyperwalletUserMock);

		final HyperwalletUser result = testObj.callHyperwalletSDKCatchingException(hyperwalletMock, USER_TOKEN);

		assertThat(result).isEqualTo(hyperwalletUserMock);
	}

	@Test
	void callHyperwalletSDKCatchingException_whenExceptionIsThrown_shouldReturnNull() {
		when(hyperwalletMock.getUser(USER_TOKEN)).thenThrow(HyperwalletException.class);

		final HyperwalletUser hyperwalletUser = testObj.callHyperwalletSDKCatchingException(hyperwalletMock,
				USER_TOKEN);

		assertThat(hyperwalletUser).isNull();
	}

	@Test
	void getHyperWalletUser_shouldCallHyperwalletOnEachUserToken() {
		when(kycHyperwalletApiConfigMock.getTokens()).thenReturn(USER_STORE_TOKENS);
		when(hyperwalletSDKUserService.getHyperwalletInstanceByHyperwalletProgram(anyString()))
				.thenReturn(hyperwalletMock);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getUserToken()).thenReturn(USER_TOKEN);
		when(hyperwalletMock.getUser(USER_TOKEN)).thenReturn(hyperwalletUserMock);

		final HyperwalletUser result = testObj
				.getHyperWalletUser(kycBusinessStakeholderStatusNotificationBodyModelMock);

		verify(hyperwalletMock, times(2)).getUser(USER_TOKEN);
		assertThat(result).isEqualTo(hyperwalletUserMock);
	}

	@Test
	void getHyperWalletUser_whenTheUserDoesNotExitInHyperWallet_shouldThrowHMCException() {
		when(kycHyperwalletApiConfigMock.getTokens()).thenReturn(USER_STORE_TOKENS);
		when(hyperwalletSDKUserService.getHyperwalletInstanceByHyperwalletProgram(anyString()))
				.thenReturn(hyperwalletMock);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getUserToken()).thenReturn(USER_TOKEN);
		when(hyperwalletMock.getUser(USER_TOKEN)).thenThrow(HyperwalletException.class);

		final Throwable throwable = catchThrowable(
				() -> testObj.getHyperWalletUser(kycBusinessStakeholderStatusNotificationBodyModelMock));

		verify(hyperwalletMock, times(2)).getUser(USER_TOKEN);
		assertThat(throwable).isInstanceOf(HMCException.class);
		assertThat(throwable.getMessage()).isEqualTo(String.format(HMC_EXCEPTION_MESSAGE, USER_TOKEN));
	}

	@Test
	void updateMiraklProofIdentityFlagStatus_whenCustomValuesForVerificationIsEmpty_shouldNotCallMiraklsAPI() {
		testObj.updateMiraklProofIdentityFlagStatus(MIRAKL_SHOP_ID, "", HyperwalletUser.VerificationStatus.REQUIRED);

		verifyNoInteractions(miraklMarketplacePlatformOperatorApiClientMock);
	}

	@Test
	void updateMiraklProofIdentityFlagStatus_whenCustomValuesForVerificationIsNull_shouldNotCallMiraklsAPI() {
		testObj.updateMiraklProofIdentityFlagStatus(MIRAKL_SHOP_ID, null, HyperwalletUser.VerificationStatus.REQUIRED);

		verifyNoInteractions(miraklMarketplacePlatformOperatorApiClientMock);
	}

	@Test
	void updateMiraklProofIdentityFlagStatus_whenStatusIsRequired_shouldUpdateMiraklWithProofOfIdentityNeeded() {
		when(miraklMarketplacePlatformOperatorApiClientMock
				.updateShops(miraklUpdateShopsRequestArgumentCaptor.capture())).thenReturn(miraklUpdatedShopsMock);

		testObj.updateMiraklProofIdentityFlagStatus(MIRAKL_SHOP_ID, BSTK_PROOF_IDENTITY_FIELD,
				HyperwalletUser.VerificationStatus.REQUIRED);

		final MiraklUpdateShopsRequest result = miraklUpdateShopsRequestArgumentCaptor.getValue();
		final List<MiraklRequestAdditionalFieldValue> additionalFieldValuesToBeChanged = result.getShops().get(0)
				.getAdditionalFieldValues();

		assertThat(additionalFieldValuesToBeChanged).hasSize(1);
		final MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue miraklSimpleRequestAdditionalFieldValue = (MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue) additionalFieldValuesToBeChanged
				.get(0);
		assertThat(miraklSimpleRequestAdditionalFieldValue.getCode()).isEqualTo(BSTK_PROOF_IDENTITY_FIELD);
		assertThat(miraklSimpleRequestAdditionalFieldValue.getValue()).isEqualTo("true");
	}

	@Test
	void updateMiraklProofIdentityFlagStatus_whenStatusIsNotRequired_shouldUpdateMiraklWithProofOfIdentityNotNeeded() {
		when(miraklMarketplacePlatformOperatorApiClientMock
				.updateShops(miraklUpdateShopsRequestArgumentCaptor.capture())).thenReturn(miraklUpdatedShopsMock);

		testObj.updateMiraklProofIdentityFlagStatus(MIRAKL_SHOP_ID, BSTK_PROOF_IDENTITY_FIELD,
				HyperwalletUser.VerificationStatus.NOT_REQUIRED);

		final MiraklUpdateShopsRequest result = miraklUpdateShopsRequestArgumentCaptor.getValue();
		final List<MiraklRequestAdditionalFieldValue> additionalFieldValuesToBeChanged = result.getShops().get(0)
				.getAdditionalFieldValues();

		assertThat(additionalFieldValuesToBeChanged).hasSize(1);
		final MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue miraklSimpleRequestAdditionalFieldValue = (MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue) additionalFieldValuesToBeChanged
				.get(0);
		assertThat(miraklSimpleRequestAdditionalFieldValue.getCode()).isEqualTo(BSTK_PROOF_IDENTITY_FIELD);
		assertThat(miraklSimpleRequestAdditionalFieldValue.getValue()).isEqualTo("false");
	}

	@Test
	void updateMiraklProofIdentityFlagStatus_whenAPICallThrowsException_shouldThrowException() {
		final MiraklException exception = new MiraklException("An error has occurred");
		when(miraklMarketplacePlatformOperatorApiClientMock
				.updateShops(miraklUpdateShopsRequestArgumentCaptor.capture())).thenThrow(exception);

		final Throwable throwable = catchThrowable(() -> testObj.updateMiraklProofIdentityFlagStatus(MIRAKL_SHOP_ID,
				BSTK_PROOF_IDENTITY_FIELD, HyperwalletUser.VerificationStatus.REQUIRED));
		assertThat(throwable).isEqualTo(exception);

		final MiraklUpdateShopsRequest result = miraklUpdateShopsRequestArgumentCaptor.getValue();
		final List<MiraklRequestAdditionalFieldValue> additionalFieldValuesToBeChanged = result.getShops().get(0)
				.getAdditionalFieldValues();

		assertThat(additionalFieldValuesToBeChanged).hasSize(1);
		final MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue miraklSimpleRequestAdditionalFieldValue = (MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue) additionalFieldValuesToBeChanged
				.get(0);
		assertThat(miraklSimpleRequestAdditionalFieldValue.getCode()).isEqualTo(BSTK_PROOF_IDENTITY_FIELD);
		assertThat(miraklSimpleRequestAdditionalFieldValue.getValue()).isEqualTo("true");
	}

	@Test
	void execute_whenTheUserDoesNotExistOnHyperwallet_shouldThrowHMCException() {
		when(kycHyperwalletApiConfigMock.getTokens()).thenReturn(USER_STORE_TOKENS);
		when(hyperwalletSDKUserService.getHyperwalletInstanceByHyperwalletProgram(anyString()))
				.thenReturn(hyperwalletMock);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getUserToken()).thenReturn(USER_TOKEN);
		when(hyperwalletMock.getUser(USER_TOKEN)).thenThrow(HyperwalletException.class);

		final Throwable throwable = catchThrowable(
				() -> testObj.execute(kycBusinessStakeholderStatusNotificationBodyModelMock));

		verify(miraklBusinessStakeholderDocumentsExtractServiceMock, never())
				.getKYCCustomValuesRequiredVerificationBusinessStakeholders(anyString(), anyList());
		assertThat(throwable).isInstanceOf(HMCException.class);
		assertThat(throwable.getMessage()).isEqualTo(String.format(HMC_EXCEPTION_MESSAGE, USER_TOKEN));
	}

	@Test
	void execute_whenTheUserExistOnHyperwalletButThereAreNoCustomFields_shouldNotCallMiraklToUpdate() {
		when(kycHyperwalletApiConfigMock.getTokens()).thenReturn(USER_STORE_TOKENS);
		when(hyperwalletSDKUserService.getHyperwalletInstanceByHyperwalletProgram(anyString()))
				.thenReturn(hyperwalletMock);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getUserToken()).thenReturn(USER_TOKEN);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getToken()).thenReturn(BSTK_TOKEN);
		when(hyperwalletMock.getUser(USER_TOKEN)).thenReturn(hyperwalletUserMock);
		when(hyperwalletUserMock.getClientUserId()).thenReturn(CLIENT_ID);
		when(miraklBusinessStakeholderDocumentsExtractServiceMock
				.getKYCCustomValuesRequiredVerificationBusinessStakeholders(anyString(), anyList()))
						.thenReturn(List.of());
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getVerificationStatus())
				.thenReturn(HyperwalletUser.VerificationStatus.REQUIRED);

		testObj.execute(kycBusinessStakeholderStatusNotificationBodyModelMock);

		verify(miraklMarketplacePlatformOperatorApiClientMock, never())
				.updateShops(isA(MiraklUpdateShopsRequest.class));
	}

	@Test
	void execute_whenTheUserExistOnHyperwalletButThereAreCustomFields_shouldCallMiraklToUpdate() {
		when(kycHyperwalletApiConfigMock.getTokens()).thenReturn(USER_STORE_TOKENS);
		when(hyperwalletSDKUserService.getHyperwalletInstanceByHyperwalletProgram(anyString()))
				.thenReturn(hyperwalletMock);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getUserToken()).thenReturn(USER_TOKEN);
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getToken()).thenReturn(BSTK_TOKEN);
		when(hyperwalletMock.getUser(USER_TOKEN)).thenReturn(hyperwalletUserMock);
		when(hyperwalletUserMock.getClientUserId()).thenReturn(CLIENT_ID);
		when(miraklBusinessStakeholderDocumentsExtractServiceMock
				.getKYCCustomValuesRequiredVerificationBusinessStakeholders(anyString(), anyList()))
						.thenReturn(List.of(CUSTOM_FIELD_1, CUSTOM_FIELD_2));
		when(kycBusinessStakeholderStatusNotificationBodyModelMock.getVerificationStatus())
				.thenReturn(HyperwalletUser.VerificationStatus.REQUIRED);

		testObj.execute(kycBusinessStakeholderStatusNotificationBodyModelMock);

		verify(miraklMarketplacePlatformOperatorApiClientMock).updateShops(isA(MiraklUpdateShopsRequest.class));
	}

}

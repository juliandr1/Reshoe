package com.project.reshoe_fbu.helper;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class PaymentsUtil {
    public static final BigDecimal CENTS_IN_A_UNIT = new BigDecimal(100d);

    private static JSONObject getBaseRequest() throws JSONException {
        return new JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0);
    }

    public static PaymentsClient createPaymentsClient(Activity activity) {
        Wallet.WalletOptions walletOptions =
                new Wallet.WalletOptions.Builder().setEnvironment(Constants.PAYMENTS_ENVIRONMENT).build();
        return Wallet.getPaymentsClient(activity, walletOptions);
    }

    private static JSONObject getGatewayTokenizationSpecification() throws JSONException {
        return new JSONObject() {{
            put("type", "PAYMENT_GATEWAY");
            put("parameters", new JSONObject() {{
                put("gateway", "example");
                put("gatewayMerchantId", "exampleGatewayMerchantId");
            }});
        }};
    }

    private static JSONObject getDirectTokenizationSpecification()
            throws JSONException, RuntimeException {
        if (Constants.DIRECT_TOKENIZATION_PARAMETERS.isEmpty()
                || Constants.DIRECT_TOKENIZATION_PUBLIC_KEY.isEmpty()
                || Constants.DIRECT_TOKENIZATION_PUBLIC_KEY == null
                || Constants.DIRECT_TOKENIZATION_PUBLIC_KEY == "REPLACE_ME") {
            throw new RuntimeException(
                    "Please edit the Constants.java file to add protocol version & public key.");
        }
        JSONObject tokenizationSpecification = new JSONObject();

        tokenizationSpecification.put("type", "DIRECT");
        JSONObject parameters = new JSONObject(Constants.DIRECT_TOKENIZATION_PARAMETERS);
        tokenizationSpecification.put("parameters", parameters);

        return tokenizationSpecification;
    }

    private static JSONArray getAllowedCardNetworks() {
        return new JSONArray(Constants.SUPPORTED_NETWORKS);
    }

    private static JSONArray getAllowedCardAuthMethods() {
        return new JSONArray(Constants.SUPPORTED_METHODS);
    }

    private static JSONObject getBaseCardPaymentMethod() throws JSONException {
        JSONObject cardPaymentMethod = new JSONObject();
        cardPaymentMethod.put("type", "CARD");

        JSONObject parameters = new JSONObject();
        parameters.put("allowedAuthMethods", getAllowedCardAuthMethods());
        parameters.put("allowedCardNetworks", getAllowedCardNetworks());
        // Optionally, you can add billing address/phone number associated with a CARD payment method.
        parameters.put("billingAddressRequired", true);

        JSONObject billingAddressParameters = new JSONObject();
        billingAddressParameters.put("format", "FULL");

        parameters.put("billingAddressParameters", billingAddressParameters);

        cardPaymentMethod.put("parameters", parameters);

        return cardPaymentMethod;
    }

    private static JSONObject getCardPaymentMethod() throws JSONException {
        JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
        cardPaymentMethod.put("tokenizationSpecification", getGatewayTokenizationSpecification());

        return cardPaymentMethod;
    }

    public static Optional<JSONObject> getIsReadyToPayRequest() {
        try {
            JSONObject isReadyToPayRequest = getBaseRequest();
            isReadyToPayRequest.put(
                    "allowedPaymentMethods", new JSONArray().put(getBaseCardPaymentMethod()));

            return Optional.of(isReadyToPayRequest);

        } catch (JSONException e) {
            return Optional.empty();
        }
    }

    private static JSONObject getTransactionInfo(String price) throws JSONException {
        JSONObject transactionInfo = new JSONObject();
        transactionInfo.put("totalPrice", price);
        transactionInfo.put("totalPriceStatus", "FINAL");
        transactionInfo.put("countryCode", Constants.COUNTRY_CODE);
        transactionInfo.put("currencyCode", Constants.CURRENCY_CODE);
        transactionInfo.put("checkoutOption", "COMPLETE_IMMEDIATE_PURCHASE");

        return transactionInfo;
    }

    private static JSONObject getMerchantInfo() throws JSONException {
        return new JSONObject().put("merchantName", "Example Merchant");
    }

    public static Optional<JSONObject> getPaymentDataRequest(long priceCents) {

        final String price = PaymentsUtil.centsToString(priceCents);

        try {
            JSONObject paymentDataRequest = PaymentsUtil.getBaseRequest();
            paymentDataRequest.put(
                    "allowedPaymentMethods", new JSONArray().put(PaymentsUtil.getCardPaymentMethod()));
            paymentDataRequest.put("transactionInfo", PaymentsUtil.getTransactionInfo(price));
            paymentDataRequest.put("merchantInfo", PaymentsUtil.getMerchantInfo());

      /* An optional shipping address requirement is a top-level property of the PaymentDataRequest
      JSON object. */
            paymentDataRequest.put("shippingAddressRequired", true);

            JSONObject shippingAddressParameters = new JSONObject();
            shippingAddressParameters.put("phoneNumberRequired", false);

            JSONArray allowedCountryCodes = new JSONArray(Constants.SHIPPING_SUPPORTED_COUNTRIES);

            shippingAddressParameters.put("allowedCountryCodes", allowedCountryCodes);
            paymentDataRequest.put("shippingAddressParameters", shippingAddressParameters);
            return Optional.of(paymentDataRequest);

        } catch (JSONException e) {
            return Optional.empty();
        }
    }

    /**
     * Converts cents to a string format accepted by {@link PaymentsUtil#getPaymentDataRequest}.
     *
     * @param cents value of the price in cents.
     */
    public static String centsToString(long cents) {
        return new BigDecimal(cents)
                .divide(CENTS_IN_A_UNIT, RoundingMode.HALF_EVEN)
                .setScale(2, RoundingMode.HALF_EVEN)
                .toString();
    }
}

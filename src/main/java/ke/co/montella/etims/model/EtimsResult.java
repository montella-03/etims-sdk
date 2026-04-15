package ke.co.montella.etims.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.montella.etims.exception.EtimsException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * High-level result wrapper returned by all {@code EtimsSdk} operations.
 *
 * <p>Provides convenient accessors for the fields most commonly needed after a
 * successful sales or credit/debit note submission: {@link #getRcptNo()},
 * {@link #getQrCode()}, {@link #getIntrlData()}, and {@link #getReceiptUrl()}.
 *
 * <h3>Checking success</h3>
 * <pre>{@code
 * EtimsResult result = sdk.sendBill(...);
 *
 * if (result.isSuccess()) {
 *     String qr     = result.getQrCode().orElse("");
 *     String rcptNo = result.getRcptNo().orElse("N/A");
 * } else {
 *     log.warn("eTIMS rejected the bill: {}", result.getResultMsg());
 * }
 * }</pre>
 *
 * <h3>Fail-fast style</h3>
 * <pre>{@code
 * EtimsResult result = sdk.sendBill(...);
 * result.throwIfFailed();   // throws EtimsException if not success
 * String qr = result.getQrCode().orElseThrow();
 * }</pre>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EtimsResult {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** {@code true} when KRA returned result code {@code "000"}. */
    private final boolean success;

    /** KRA result code, e.g. {@code "000"} for success or {@code "101"} for failure. */
    private final String resultCd;

    /** Human-readable result message from KRA. */
    private final String resultMsg;

    /** Timestamp string returned by KRA ({@code yyyyMMddHHmmss} format). */
    private final String resultDt;

    /** KRA fiscal receipt number assigned to this transaction. */
    private final String rcptNo;

    /**
     * QR code string to print on the patient/customer receipt.
     * Encode this value into a QR image using any standard QR library.
     */
    private final String qrCode;

    /**
     * Internal signed data returned by KRA ({@code intrlData}).
     * Store this alongside the transaction for audit purposes.
     */
    private final String intrlData;

    /** Optional receipt URL provided by KRA (may be {@code null}). */
    private final String receiptUrl;

    // -----------------------------------------------------------------
    // Optional accessors (null-safe)
    // -----------------------------------------------------------------

    /**
     * Returns the KRA fiscal receipt number, if present.
     *
     * @return {@code Optional} containing the receipt number
     */
    public Optional<String> getRcptNo() {
        return Optional.ofNullable(rcptNo);
    }

    /**
     * Returns the QR code string for receipt printing, if present.
     *
     * @return {@code Optional} containing the QR code value
     */
    public Optional<String> getQrCode() {
        return Optional.ofNullable(qrCode);
    }

    /**
     * Returns the KRA internal signed data ({@code intrlData}), if present.
     *
     * @return {@code Optional} containing the signed data
     */
    public Optional<String> getIntrlData() {
        return Optional.ofNullable(intrlData);
    }

    /**
     * Returns the KRA receipt URL, if provided.
     *
     * @return {@code Optional} containing the receipt URL
     */
    public Optional<String> getReceiptUrl() {
        return Optional.ofNullable(receiptUrl);
    }

    // -----------------------------------------------------------------
    // Fail-fast helper
    // -----------------------------------------------------------------

    /**
     * Throws {@link EtimsException} if this result is not successful.
     *
     * <p>Use this for fail-fast workflows where an unsuccessful response
     * should be treated as an exception rather than a branch condition.
     *
     * @return {@code this} for chaining (only when successful)
     * @throws EtimsException if {@link #isSuccess()} is {@code false}
     */
    public EtimsResult throwIfFailed() {
        if (!success) {
            throw new EtimsException(resultCd, resultMsg);
        }
        return this;
    }

    // -----------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------

    /**
     * Builds an {@code EtimsResult} from the raw {@link EtimsResponse} returned
     * by the HTTP layer.  Extracts {@code rcptNo}, {@code qrCode},
     * {@code intrlData}, and {@code receiptUrl} from the nested {@code data}
     * node when the call was successful.
     *
     * @param response raw API response
     * @return parsed result
     */
    public static EtimsResult from(EtimsResponse<Object> response) {
        boolean success = "000".equals(response.getResultCd());

        String rcptNo = null;
        String qrCode = null;
        String intrlData = null;
        String receiptUrl = null;

        if (success && response.getData() != null) {
            JsonNode dataNode = MAPPER.valueToTree(response.getData());
            rcptNo      = nullIfEmpty(dataNode.path("rcptNo").asText(null));
            qrCode      = nullIfEmpty(dataNode.path("qrCode").asText(null));
            intrlData   = nullIfEmpty(dataNode.path("intrlData").asText(null));
            receiptUrl  = nullIfEmpty(dataNode.path("receiptUrl").asText(null));
        }

        return new EtimsResult(
                success,
                response.getResultCd(),
                response.getResultMsg(),
                response.getResultDt(),
                rcptNo,
                qrCode,
                intrlData,
                receiptUrl
        );
    }

    /**
     * Creates a failure result without a raw response (e.g. for local validation errors).
     *
     * @param resultCd KRA-style result code
     * @param message  error message
     * @return failure result
     */
    public static EtimsResult failure(String resultCd, String message) {
        return new EtimsResult(false, resultCd, message, null, null, null, null, null);
    }

    private static String nullIfEmpty(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    @Override
    public String toString() {
        return "EtimsResult{success=" + success
                + ", resultCd='" + resultCd + '\''
                + ", resultMsg='" + resultMsg + '\''
                + ", rcptNo='" + rcptNo + '\''
                + '}';
    }
}

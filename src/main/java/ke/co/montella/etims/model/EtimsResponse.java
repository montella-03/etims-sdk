package ke.co.montella.etims.model;

import lombok.Data;

/**
 * Raw JSON response envelope returned by all KRA eTIMS API endpoints.
 *
 * <p>Every response from the KRA eTIMS REST API wraps its payload in this
 * common envelope.  The generic type parameter {@code T} represents the
 * endpoint-specific {@code data} payload — its structure varies per endpoint.
 *
 * <p>In most cases you should use {@link EtimsResult} instead of working with
 * this class directly.  {@code EtimsResult} is built from this response via
 * {@link EtimsResult#from(EtimsResponse)} and exposes convenient accessors for
 * the most commonly needed fields ({@code rcptNo}, {@code qrCode}, etc.).
 *
 * <h3>Result codes</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>Code</th><th>Meaning</th></tr>
 *   <tr><td>{@code 000}</td><td>Success</td></tr>
 *   <tr><td>{@code 101}</td><td>Unauthorized / invalid credentials</td></tr>
 *   <tr><td>{@code 201}</td><td>Duplicate invoice number</td></tr>
 *   <tr><td>{@code 500}</td><td>KRA internal server error</td></tr>
 * </table>
 *
 * @param <T> type of the {@code data} payload
 */
@Data
public class EtimsResponse<T> {

    /** KRA result code. {@code "000"} indicates success. */
    private String resultCd;

    /** Human-readable result message from KRA. */
    private String resultMsg;

    /** Timestamp of the response ({@code yyyyMMddHHmmss} format). */
    private String resultDt;

    /**
     * Endpoint-specific response payload.
     *
     * <p>For {@code sendSalesTrns} this contains {@code rcptNo},
     * {@code qrCode}, {@code intrlData}, and {@code receiptUrl}.
     * For {@code selectInitOsdcInfo} it contains {@code info.cmcKey}.
     */
    private T data;
}

package ke.co.montella.etims.model;

import lombok.Data;

/**
 * montella-03
 * Raw data payload returned inside {@link EtimsResponse} for
 * {@code sendSalesTrns} calls.
 *
 * <p><strong>Prefer {@link EtimsResult}</strong> over this class.
 * {@link EtimsResult} is built automatically from the raw response and
 * exposes {@code Optional}-based accessors for {@code rcptNo},
 * {@code qrCode}, {@code intrlData}, and {@code receiptUrl}, as well as
 * a {@code throwIfFailed()} convenience method.
 *
 * <p>This class is retained for applications that need to inspect the raw
 * response fields directly.
 *
 * @see EtimsResult
 */
@Data
public class EtimsApiResponse {

    /** KRA result code ({@code "000"} = success). */
    private String resultCd;

    /** Human-readable result message. */
    private String resultMsg;

    /** Response timestamp ({@code yyyyMMddHHmmss}). */
    private String resultDt;

    /** KRA fiscal receipt number assigned to the transaction. */
    private String rcptNo;

    /** Internal signed data ({@code intrlData}) — store for audit purposes. */
    private String intrlData;

    /** QR code string to encode and print on the receipt. */
    private String qrCode;

    /** Optional receipt URL provided by KRA. */
    private String receiptUrl;
}

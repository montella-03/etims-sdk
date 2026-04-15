package ke.co.montella.etims.exception;

/**
 * Base runtime exception for all KRA eTIMS SDK errors.
 *
 * <p>All checked failures—network errors, API error codes, invalid state—are
 * wrapped in (or thrown as) an {@code EtimsException} so callers can catch a
 * single type without declaring checked exceptions.
 *
 * <h3>Common causes</h3>
 * <ul>
 *   <li>Device not yet initialized (CMC Key missing)</li>
 *   <li>KRA API returned a non-{@code 000} result code</li>
 *   <li>Network timeout or connectivity issue</li>
 *   <li>Malformed JSON in API response</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * try {
 *     EtimsResult result = sdk.sendBill(...);
 * } catch (EtimsException ex) {
 *     log.error("eTIMS error [{}]: {}", ex.getResultCd(), ex.getMessage());
 * }
 * }</pre>
 */
public class EtimsException extends RuntimeException {

    /** KRA result code, e.g. {@code "101"}, or {@code null} if the error is not API-level. */
    private final String resultCd;

    /**
     * Creates an exception with a message and no KRA result code.
     *
     * @param message human-readable error description
     */
    public EtimsException(String message) {
        super(message);
        this.resultCd = null;
    }

    /**
     * Creates an exception with a message and an underlying cause.
     *
     * @param message human-readable error description
     * @param cause   the original exception
     */
    public EtimsException(String message, Throwable cause) {
        super(message, cause);
        this.resultCd = null;
    }

    /**
     * Creates an exception that carries the KRA API result code.
     *
     * @param resultCd KRA result code (e.g. {@code "101"})
     * @param message  KRA result message
     */
    public EtimsException(String resultCd, String message) {
        super(message + " [resultCd=" + resultCd + "]");
        this.resultCd = resultCd;
    }

    /**
     * Returns the KRA API result code, or {@code null} if not applicable.
     *
     * @return KRA result code or {@code null}
     */
    public String getResultCd() {
        return resultCd;
    }
}

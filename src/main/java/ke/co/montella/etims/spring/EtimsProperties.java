package ke.co.montella.etims.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for the KRA eTIMS SDK.
 *
 * <p>All properties are bound under the {@code etims} prefix in
 * {@code application.properties} / {@code application.yml}.
 *
 * <h3>application.properties example</h3>
 * <pre>
 * # Required
 * etims.tin=P051234567X
 * etims.bhf-id=00
 * etims.device-srl-no=SN-123456
 *
 * # Environment (default: false = sandbox)
 * etims.production=false
 *
 * # VSCU mode (default: false = direct OSCU)
 * etims.use-vscu=false
 * etims.vscu-base-url=http://localhost:8088/
 *
 * # Auto-initialize on startup (default: true)
 * etims.auto-initialize=true
 * </pre>
 *
 * <h3>application.yml example</h3>
 * <pre>
 * etims:
 *   tin: P051234567X
 *   bhf-id: "00"
 *   device-srl-no: SN-123456
 *   production: false
 *   auto-initialize: true
 * </pre>
 *
 * @see EtimsAutoConfiguration
 */
@Data
@ConfigurationProperties(prefix = "etims")
public class EtimsProperties {

    /**
     * KRA Taxpayer Identification Number (TIN / KRA PIN).
     * <p><strong>Required.</strong>
     */
    private String tin;

    /**
     * Branch ID assigned by KRA.
     * <p>Defaults to {@code "00"} for single-branch businesses.
     */
    private String bhfId = "00";

    /**
     * Hardware device serial number registered with KRA.
     * <p><strong>Required.</strong>
     */
    private String deviceSrlNo;

    /**
     * Set to {@code true} to target the KRA production endpoint.
     * <p>Defaults to {@code false} (sandbox).
     */
    private boolean production = false;

    /**
     * Set to {@code true} to route requests to a local VSCU device instead
     * of the KRA cloud endpoint.
     * <p>Defaults to {@code false}.
     */
    private boolean useVscu = false;

    /**
     * Base URL of the local VSCU device.
     * <p>Only used when {@link #isUseVscu()} is {@code true}.
     * Defaults to {@code http://localhost:8088/}.
     */
    private String vscuBaseUrl = "http://localhost:8088/";

    /**
     * When {@code true} (default), the SDK calls {@code EtimsSdk.initialize()}
     * automatically during Spring Boot application startup.
     * <p>Set to {@code false} if you want to control initialization timing
     * manually (e.g. in a {@code @PostConstruct} method or health-check).
     */
    private boolean autoInitialize = true;
}

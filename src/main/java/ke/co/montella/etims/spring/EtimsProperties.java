package ke.co.montella.etims.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot configuration properties for the KRA eTIMS SDK.
 *
 * <p>All properties are bound under the {@code etims} prefix in
 * {@code application.properties} / {@code application.yml}.
 *
 * <h3>Single-tenant (application.properties)</h3>
 * <pre>
 * etims.tin=P051234567X
 * etims.bhf-id=00
 * etims.device-srl-no=SN-123456
 * etims.production=false
 * etims.auto-initialize=true
 * </pre>
 *
 * <h3>Multi-tenant (application.yml)</h3>
 * <pre>
 * etims:
 *   production: true          # root default — inherited by all tenants
 *   auto-initialize: true
 *   default-tenant: nairobi
 *   tenants:
 *     nairobi:
 *       tin: P051234567X
 *       bhf-id: "00"
 *       device-srl-no: SN-001
 *     mombasa:
 *       tin: P051234567X
 *       bhf-id: "01"
 *       device-srl-no: SN-002
 *     sandbox:
 *       tin: P051234567X
 *       bhf-id: "00"
 *       device-srl-no: SN-TEST
 *       production: false       # overrides the root default for this tenant
 *       auto-initialize: false
 * </pre>
 *
 * <p>When {@code etims.tenants} is non-empty the SDK registers an
 * {@link EtimsSdkRegistry} bean instead of a single
 * {@link ke.co.montella.etims.EtimsSdk} bean.  Per-tenant fields that are
 * left blank inherit the corresponding root-level value.
 *
 * @see EtimsAutoConfiguration
 * @see EtimsSdkRegistry
 */
@Data
@ConfigurationProperties(prefix = "etims")
public class EtimsProperties {

    // ---------------------------------------------------------------
    // Root / single-tenant fields  (also serve as defaults for tenants)
    // ---------------------------------------------------------------

    /**
     * KRA Taxpayer Identification Number (TIN / KRA PIN).
     * <p><strong>Required</strong> in single-tenant mode.  In multi-tenant
     * mode each {@link TenantProperties} entry must provide its own TIN unless
     * all tenants share the same TIN, in which case it can be set here as a
     * default.
     */
    private String tin;

    /**
     * Branch ID assigned by KRA.
     * <p>Defaults to {@code "00"}.  Acts as the fallback for any tenant that
     * does not specify its own {@code bhf-id}.
     */
    private String bhfId = "00";

    /**
     * Hardware device serial number registered with KRA.
     * <p><strong>Required</strong> in single-tenant mode.
     */
    private String deviceSrlNo;

    /**
     * Set to {@code true} to target the KRA production endpoint.
     * <p>Defaults to {@code false} (sandbox).  Tenants may override this.
     */
    private boolean production = false;

    /**
     * Set to {@code true} to route requests to a local VSCU device instead
     * of the KRA cloud endpoint.
     * <p>Defaults to {@code false}.  Tenants may override this.
     */
    private boolean useVscu = false;

    /**
     * Base URL of the local VSCU device.
     * <p>Only used when {@link #isUseVscu()} is {@code true}.
     * Defaults to {@code http://localhost:8088/}.  Tenants may override this.
     */
    private String vscuBaseUrl = "http://localhost:8088/";

    /**
     * Integration token (Bearer token) required by the VSCU device.
     * <p>Only used when {@link #isUseVscu()} is {@code true}.  Obtain this
     * value from your VSCU device's management interface and set it via:
     * <pre>etims.vscu-integration-token=your-token-here</pre>
     * Tenants may override this per-tenant.
     */
    private String vscuIntegrationToken;

    /**
     * When {@code true} (default), the SDK calls {@code EtimsSdk.initialize()}
     * automatically during Spring Boot application startup.
     * <p>Set to {@code false} to control initialization timing manually.
     * Tenants may override this per-tenant.
     */
    private boolean autoInitialize = true;

    // ---------------------------------------------------------------
    // Multi-tenant fields
    // ---------------------------------------------------------------

    /**
     * Key of the tenant to expose as the default via
     * {@link EtimsSdkRegistry#getDefaultSdk()}.
     * <p>Only used in multi-tenant mode ({@code etims.tenants} is non-empty).
     */
    private String defaultTenant;

    /**
     * Named tenant configurations.
     *
     * <p>When this map is non-empty the SDK operates in <em>multi-tenant
     * mode</em>: one {@link ke.co.montella.etims.EtimsSdk} is created per
     * entry and an {@link EtimsSdkRegistry} bean is registered.  The single
     * {@link ke.co.montella.etims.EtimsSdk} bean is <strong>not</strong>
     * created in this mode — inject {@link EtimsSdkRegistry} instead.
     *
     * <p>Any field left {@code null} on a tenant entry inherits the
     * corresponding root-level value.
     */
    private Map<String, TenantProperties> tenants = new LinkedHashMap<>();

    // ---------------------------------------------------------------
    // Inner class
    // ---------------------------------------------------------------

    /**
     * Per-tenant configuration overrides.
     *
     * <p>All fields are optional; a {@code null} value means "inherit the
     * root-level default".  Only {@code tin} and {@code device-srl-no}
     * <em>must</em> be provided if they differ from the root-level values.
     */
    @Data
    public static class TenantProperties {

        /** TIN for this tenant. {@code null} → inherit {@code etims.tin}. */
        private String tin;

        /** Branch ID for this tenant. {@code null} → inherit {@code etims.bhf-id}. */
        private String bhfId;

        /** Device serial for this tenant. {@code null} → inherit {@code etims.device-srl-no}. */
        private String deviceSrlNo;

        /** {@code null} → inherit {@code etims.production}. */
        private Boolean production;

        /** {@code null} → inherit {@code etims.use-vscu}. */
        private Boolean useVscu;

        /** {@code null} → inherit {@code etims.vscu-base-url}. */
        private String vscuBaseUrl;

        /** {@code null} → inherit {@code etims.vscu-integration-token}. */
        private String vscuIntegrationToken;

        /** {@code null} → inherit {@code etims.auto-initialize}. */
        private Boolean autoInitialize;
    }
}

package ke.co.montella.etims.spring;

import ke.co.montella.etims.EtimsConfig;
import ke.co.montella.etims.EtimsSdk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot auto-configuration for the KRA eTIMS SDK.
 *
 * <p>When the SDK JAR is on the classpath this class detects whether
 * single-tenant or multi-tenant properties are present and registers the
 * appropriate beans automatically.
 *
 * <h3>Single-tenant mode</h3>
 * <p>Set the flat {@code etims.*} properties and inject {@link EtimsSdk}:
 * <pre>
 * etims.tin=P051234567X
 * etims.bhf-id=00
 * etims.device-srl-no=SN-123456
 * </pre>
 * <pre>{@code
 * @Service
 * public class BillingService {
 *     private final EtimsSdk etimsSdk;
 *     ...
 * }
 * }</pre>
 *
 * <h3>Multi-tenant mode</h3>
 * <p>Populate {@code etims.tenants} and inject {@link EtimsSdkRegistry}:
 * <pre>
 * etims:
 *   production: true
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
 * </pre>
 * <pre>{@code
 * @Service
 * public class BillingService {
 *     private final EtimsSdkRegistry etimsRegistry;
 *
 *     public void bill(String facilityId, ...) {
 *         EtimsSdk sdk = etimsRegistry.getSdk(facilityId);
 *     }
 * }
 * }</pre>
 *
 * <h3>Overriding beans</h3>
 * All beans carry {@code @ConditionalOnMissingBean}, so you can define your
 * own {@link EtimsConfig}, {@link EtimsSdk}, or {@link EtimsSdkRegistry} bean
 * to replace the auto-configured defaults.
 *
 * @see EtimsProperties
 * @see EtimsSdkRegistry
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(EtimsProperties.class)
public class EtimsAutoConfiguration {

    // ---------------------------------------------------------------
    // Single-tenant path
    // ---------------------------------------------------------------

    /**
     * Single-tenant configuration — active when {@code etims.tenants} is empty.
     */
    @Configuration(proxyBeanMethods = false)
    @Conditional(SingleTenantCondition.class)
    static class SingleTenantConfiguration {

        /**
         * Creates an {@link EtimsConfig} bean populated from the flat
         * {@link EtimsProperties}.
         *
         * @param properties bound configuration properties
         * @return configured {@link EtimsConfig}
         */
        @Bean
        @ConditionalOnMissingBean
        public EtimsConfig etimsConfig(EtimsProperties properties) {
            EtimsConfig config = new EtimsConfig();
            config.setTin(properties.getTin());
            config.setBhfId(properties.getBhfId());
            config.setDeviceSrlNo(properties.getDeviceSrlNo());
            config.setProduction(properties.isProduction());
            config.setUseVscu(properties.isUseVscu());
            config.setVscuBaseUrl(properties.getVscuBaseUrl());
            config.setVscuIntegrationToken(properties.getVscuIntegrationToken());
            return config;
        }

        /**
         * Creates an {@link EtimsSdk} bean, optionally auto-initializing it.
         *
         * @param config     the {@link EtimsConfig} bean
         * @param properties bound configuration properties
         * @return initialized (or ready-to-initialize) {@link EtimsSdk} bean
         */
        @Bean
        @ConditionalOnMissingBean
        public EtimsSdk etimsSdk(EtimsConfig config, EtimsProperties properties) {
            EtimsSdk sdk = new EtimsSdk(config);

            if (properties.isAutoInitialize()) {
                log.info("etims.auto-initialize=true — initializing eTIMS device at startup...");
                boolean ok = sdk.initialize();
                if (!ok) {
                    log.warn("eTIMS auto-initialization failed. "
                            + "Ensure TIN, BHF ID, and device serial are correct. "
                            + "You can retry by calling EtimsSdk.initialize() manually.");
                }
            } else {
                log.info("etims.auto-initialize=false — skipping auto-initialization. "
                        + "Call EtimsSdk.initialize() before sending transactions.");
            }

            return sdk;
        }
    }

    // ---------------------------------------------------------------
    // Multi-tenant path
    // ---------------------------------------------------------------

    /**
     * Multi-tenant configuration — active when {@code etims.tenants} is non-empty.
     */
    @Configuration(proxyBeanMethods = false)
    @Conditional(MultiTenantCondition.class)
    static class MultiTenantConfiguration {

        /**
         * Creates an {@link EtimsSdkRegistry} bean with one {@link EtimsSdk}
         * per configured tenant.
         *
         * <p>Each tenant's SDK is built from its own {@link EtimsConfig},
         * inheriting any root-level defaults for fields not explicitly set on
         * the tenant entry.  Auto-initialization follows the per-tenant
         * {@code auto-initialize} flag, falling back to the root flag.
         *
         * @param properties bound configuration properties
         * @return populated {@link EtimsSdkRegistry}
         */
        @Bean
        @ConditionalOnMissingBean
        public EtimsSdkRegistry etimsSdkRegistry(EtimsProperties properties) {
            Map<String, EtimsSdk> sdks = new LinkedHashMap<>();

            for (Map.Entry<String, EtimsProperties.TenantProperties> entry
                    : properties.getTenants().entrySet()) {

                String tenantId = entry.getKey();
                EtimsProperties.TenantProperties tenant = entry.getValue();

                EtimsConfig config = buildTenantConfig(properties, tenant);
                EtimsSdk sdk = new EtimsSdk(config);

                boolean autoInit = tenant.getAutoInitialize() != null
                        ? tenant.getAutoInitialize()
                        : properties.isAutoInitialize();

                if (autoInit) {
                    log.info("etims[{}]: auto-initializing...", tenantId);
                    boolean ok = sdk.initialize();
                    if (!ok) {
                        log.warn("etims[{}]: auto-initialization failed. "
                                + "Retry with EtimsSdkRegistry.getSdk(\"{}\").initialize().",
                                tenantId, tenantId);
                    }
                } else {
                    log.info("etims[{}]: auto-initialize=false — skipping.", tenantId);
                }

                sdks.put(tenantId, sdk);
            }

            log.info("eTIMS multi-tenant mode: {} tenant(s) registered {}",
                    sdks.size(), sdks.keySet());

            return new EtimsSdkRegistry(sdks, properties.getDefaultTenant());
        }

        private EtimsConfig buildTenantConfig(EtimsProperties root,
                                              EtimsProperties.TenantProperties tenant) {
            EtimsConfig config = new EtimsConfig();
            config.setTin(tenant.getTin() != null ? tenant.getTin() : root.getTin());
            config.setBhfId(tenant.getBhfId() != null ? tenant.getBhfId() : root.getBhfId());
            config.setDeviceSrlNo(tenant.getDeviceSrlNo() != null
                    ? tenant.getDeviceSrlNo() : root.getDeviceSrlNo());
            config.setProduction(tenant.getProduction() != null
                    ? tenant.getProduction() : root.isProduction());
            config.setUseVscu(tenant.getUseVscu() != null
                    ? tenant.getUseVscu() : root.isUseVscu());
            config.setVscuBaseUrl(tenant.getVscuBaseUrl() != null
                    ? tenant.getVscuBaseUrl() : root.getVscuBaseUrl());
            config.setVscuIntegrationToken(tenant.getVscuIntegrationToken() != null
                    ? tenant.getVscuIntegrationToken() : root.getVscuIntegrationToken());
            return config;
        }
    }

    // ---------------------------------------------------------------
    // Conditions
    // ---------------------------------------------------------------

    /**
     * Matches when no {@code etims.tenants.*} properties are present
     * (single-tenant mode).
     */
    static class SingleTenantCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context,
                                                AnnotatedTypeMetadata metadata) {
            boolean hasTenantsConfig = hasTenantsProperties(context);
            if (hasTenantsConfig) {
                return ConditionOutcome.noMatch(ConditionMessage
                        .forCondition("EtimsSingleTenantCondition")
                        .found("etims.tenants.*").items("multi-tenant mode active"));
            }
            return ConditionOutcome.match(ConditionMessage
                    .forCondition("EtimsSingleTenantCondition")
                    .didNotFind("etims.tenants.*").atAll());
        }
    }

    /**
     * Matches when at least one {@code etims.tenants.*} property is present
     * (multi-tenant mode).
     */
    static class MultiTenantCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context,
                                                AnnotatedTypeMetadata metadata) {
            boolean hasTenantsConfig = hasTenantsProperties(context);
            if (hasTenantsConfig) {
                return ConditionOutcome.match(ConditionMessage
                        .forCondition("EtimsMultiTenantCondition")
                        .found("etims.tenants.*").items("multi-tenant mode active"));
            }
            return ConditionOutcome.noMatch(ConditionMessage
                    .forCondition("EtimsMultiTenantCondition")
                    .didNotFind("etims.tenants.*").atAll());
        }
    }

    /**
     * Returns {@code true} if any property source contains at least one key
     * that starts with {@code etims.tenants.}.
     */
    private static boolean hasTenantsProperties(ConditionContext context) {
        if (!(context.getEnvironment() instanceof ConfigurableEnvironment env)) {
            return false;
        }
        return env.getPropertySources().stream()
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> (EnumerablePropertySource<?>) ps)
                .anyMatch(ps -> Arrays.stream(ps.getPropertyNames())
                        .anyMatch(name -> name.startsWith("etims.tenants.")));
    }
}
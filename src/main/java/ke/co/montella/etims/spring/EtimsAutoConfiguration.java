package ke.co.montella.etims.spring;

import ke.co.montella.etims.EtimsConfig;
import ke.co.montella.etims.EtimsSdk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the KRA eTIMS SDK.
 *
 * <p>When the SDK JAR is on the Spring Boot classpath, this class registers
 * an {@link EtimsConfig} bean and an {@link EtimsSdk} bean automatically.
 * No {@code @Import} or {@code @ComponentScan} is required in the host
 * application.
 *
 * <h3>Minimal setup</h3>
 * Add to {@code application.properties}:
 * <pre>
 * etims.tin=P051234567X
 * etims.bhf-id=00
 * etims.device-srl-no=SN-123456
 * </pre>
 * Then inject {@link EtimsSdk} anywhere:
 * <pre>{@code
 * @Service
 * public class BillingService {
 *     private final EtimsSdk etimsSdk;
 *
 *     public BillingService(EtimsSdk etimsSdk) {
 *         this.etimsSdk = etimsSdk;
 *     }
 * }
 * }</pre>
 *
 * <h3>Overriding beans</h3>
 * Both beans are annotated {@code @ConditionalOnMissingBean}, so you can
 * supply your own {@link EtimsConfig} or {@link EtimsSdk} bean to replace the
 * defaults without disabling the rest of the auto-configuration.
 *
 * @see EtimsProperties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(EtimsProperties.class)
public class EtimsAutoConfiguration {

    /**
     * Creates an {@link EtimsConfig} bean populated from {@link EtimsProperties}.
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
        return config;
    }

    /**
     * Creates an {@link EtimsSdk} bean, optionally auto-initializing it.
     *
     * <p>If {@code etims.auto-initialize=true} (the default), the bean
     * calls {@link EtimsSdk#initialize()} immediately so the CMC Key is ready
     * before the application begins serving traffic.  Initialization failures
     * are logged as warnings rather than crashing the application context —
     * this lets the app start and retry later via a health-check or scheduled
     * task.
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

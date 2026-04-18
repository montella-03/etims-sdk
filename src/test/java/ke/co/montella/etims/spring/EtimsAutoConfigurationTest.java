package ke.co.montella.etims.spring;

import ke.co.montella.etims.EtimsClient;
import ke.co.montella.etims.EtimsConfig;
import ke.co.montella.etims.EtimsSdk;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EtimsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EtimsAutoConfiguration.class));

    @Test
    void multiTenantUsesTenantSpecificVscuIntegrationToken() {
        contextRunner
                .withPropertyValues(
                        "etims.auto-initialize=false",
                        "etims.use-vscu=true",
                        "etims.vscu-integration-token=root-token",
                        "etims.tenants.nairobi.tin=P051234567X",
                        "etims.tenants.nairobi.device-srl-no=SN-001",
                        "etims.tenants.nairobi.vscu-integration-token=nairobi-token",
                        "etims.tenants.mombasa.tin=P051234567X",
                        "etims.tenants.mombasa.device-srl-no=SN-002",
                        "etims.tenants.mombasa.vscu-integration-token=mombasa-token")
                .run(context -> {
                    EtimsSdkRegistry registry = context.getBean(EtimsSdkRegistry.class);

                    assertEquals("nairobi-token",
                            extractConfig(registry.getSdk("nairobi")).getVscuIntegrationToken());
                    assertEquals("mombasa-token",
                            extractConfig(registry.getSdk("mombasa")).getVscuIntegrationToken());
                });
    }

    @Test
    void multiTenantFallsBackToRootVscuIntegrationTokenWhenTenantDoesNotOverride() {
        contextRunner
                .withPropertyValues(
                        "etims.auto-initialize=false",
                        "etims.use-vscu=true",
                        "etims.vscu-integration-token=root-token",
                        "etims.tenants.nairobi.tin=P051234567X",
                        "etims.tenants.nairobi.device-srl-no=SN-001")
                .run(context -> {
                    EtimsSdkRegistry registry = context.getBean(EtimsSdkRegistry.class);

                    assertEquals("root-token",
                            extractConfig(registry.getSdk("nairobi")).getVscuIntegrationToken());
                });
    }

    private static EtimsConfig extractConfig(EtimsSdk sdk) {
        try {
            Field configField = EtimsClient.class.getDeclaredField("config");
            configField.setAccessible(true);
            return (EtimsConfig) configField.get(sdk.getClient());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Failed to inspect EtimsConfig from SDK", ex);
        }
    }
}

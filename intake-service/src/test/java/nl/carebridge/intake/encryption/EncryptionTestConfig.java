package nl.carebridge.intake.encryption;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Database slice tests load no components by themselves, but the entities and migrations need these. */
@TestConfiguration
@ComponentScan(basePackageClasses = DataProtection.class)
public class EncryptionTestConfig {
}

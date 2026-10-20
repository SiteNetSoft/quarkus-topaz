package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Validates the enforcement configuration at startup.
 */
@Recorder
public class TopazSecurityRecorder {

    private static final Pattern PERMISSION_POLICY = Pattern.compile("quarkus\\.http\\.auth\\.permission\\..+\\.policy");

    private final RuntimeValue<TopazEnforcementConfig> config;

    public TopazSecurityRecorder(RuntimeValue<TopazEnforcementConfig> config) {
        this.config = config;
    }

    /**
     * Fails startup when the policy root is unset but a derived policy path is needed.
     *
     * @param policyRootRequired whether the application uses the {@code topaz} policy through an annotation
     * @param customPathMapper whether the application supplies its own {@code PolicyPathMapper}, which needs no
     *        policy root
     */
    public void validate(boolean policyRootRequired, boolean customPathMapper) {
        if (customPathMapper || config.getValue().policyRoot().isPresent()) {
            return;
        }
        if (policyRootRequired || permissionUsesTopaz()) {
            throw new ConfigurationException("quarkus.topaz.enforcement.policy-root must be set when the 'topaz' HTTP"
                    + " security policy or a derived @TopazAuthorize path is used");
        }
    }

    private static boolean permissionUsesTopaz() {
        Config c = ConfigProvider.getConfig();
        for (String name : c.getPropertyNames()) {
            if (PERMISSION_POLICY.matcher(name).matches()
                    && c.getOptionalValue(name, String.class).filter(TopazHttpSecurityPolicy.NAME::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }
}

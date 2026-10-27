package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.regex.Pattern;

import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * The recorder that validates the enforcement configuration at startup.
 */
@Recorder
public class TopazSecurityRecorder {

    private static final Pattern PERMISSION_POLICY = Pattern.compile("quarkus\\.http\\.auth\\.permission\\..+\\.policy");

    private final RuntimeValue<TopazEnforcementConfig> config;

    /**
     * Create the recorder.
     *
     * @param config the runtime enforcement configuration
     */
    public TopazSecurityRecorder(RuntimeValue<TopazEnforcementConfig> config) {
        this.config = config;
    }

    /**
     * Fail startup when the policy root is unset but a derived policy path is needed.
     *
     * @param policyRootRequired whether the application uses the {@code topaz} policy through an annotation
     */
    public void validate(boolean policyRootRequired) {
        if (config.getValue().policyRoot().isPresent() || hasCustomPathMapper()) {
            return;
        }
        if (policyRootRequired || permissionUsesTopaz()) {
            throw new ConfigurationException("quarkus.topaz.enforcement.policy-root must be set when the 'topaz' HTTP"
                    + " security policy or a derived @TopazAuthorize path is used");
        }
    }

    // The application supplies its own mapper when the resolved bean is not the default one.
    // A custom mapper derives the path itself, so it needs no policy root.
    // Reading the bean class does not create the instance.
    private static boolean hasCustomPathMapper() {
        Instance<PolicyPathMapper> mapper = Arc.container().select(PolicyPathMapper.class);
        return mapper.isResolvable() && !DefaultPolicyPathMapper.class.equals(mapper.getHandle().getBean().getBeanClass());
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

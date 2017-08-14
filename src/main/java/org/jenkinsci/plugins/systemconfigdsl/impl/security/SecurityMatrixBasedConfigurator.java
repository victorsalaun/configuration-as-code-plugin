package org.jenkinsci.plugins.systemconfigdsl.impl.security;

import com.google.auto.service.AutoService;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;
import org.jenkinsci.plugins.systemconfigdsl.impl.security.generated.SecurityMatrixBasedConfig;
import org.jenkinsci.plugins.systemconfigdsl.impl.security.generated.SecurityMatrixUsers;

import java.util.logging.Logger;

/**
 *
 */
@AutoService(Configurator.class)
public class SecurityMatrixBasedConfigurator extends Configurator{
    private static final Logger LOGGER = Logger.getLogger(SecurityMatrixBasedConfigurator.class.getName());
    private final String pluginName = "securityMatrixBased";

    @Override
    public String getConfigFileSectionName() {
        return this.pluginName;
    }

    @Override
    public void configure(String config, boolean dryRun) {
        LOGGER.info("Configuring Matrix based security: " + config);
        if (dryRun) {
            LOGGER.info("DryRun: Only print what you will change");
            // TODO: add printout to UI
        } else {
            GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
            final SecurityMatrixBasedConfig configObject = (SecurityMatrixBasedConfig) super.parseConfiguration(config, SecurityMatrixBasedConfig.class);
            for(SecurityMatrixUsers user : configObject.getUsers()){
                LOGGER.info("The current user name: " + user.getUserId());
                for (String name : user.getPermissions()) {
                    LOGGER.info("The current permission name: " + name);
                    strategy.add(Permission.fromId(name), user.getUserId());
                }
            }
            Jenkins.getInstance().setAuthorizationStrategy(strategy);
            LOGGER.info("Applying configuration...");
        }

    }

    @Override
    public boolean isConfigurationValid(String config) {
        return false;
    }
}

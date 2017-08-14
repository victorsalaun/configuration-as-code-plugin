package org.jenkinsci.plugins.systemconfigdsl.impl.security;

import com.google.auto.service.AutoService;
import hudson.security.HudsonPrivateSecurityRealm;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;
import org.jenkinsci.plugins.systemconfigdsl.impl.security.generated.SecurityOwnDBConfig;
import org.jenkinsci.plugins.systemconfigdsl.impl.security.generated.SecurityOwnDBUsers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 *
 */
@AutoService(Configurator.class)
public class SecurityConfigurator extends Configurator{

    private static final Logger LOGGER = Logger.getLogger(SecurityConfigurator.class.getName());
    private final String pluginName = "securityOwnDB";
    @Override
    public String getConfigFileSectionName() {
        return this.pluginName;
    }

    @Override
    public void configure(String config, boolean dryRun) {
        LOGGER.info("Configuring OwnDB security: " + config.toString());
        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
            // TODO: add printout to UI
        } else {
            HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false, false, null);
            final SecurityOwnDBConfig configObject = (SecurityOwnDBConfig) super.parseConfiguration(config, SecurityOwnDBConfig.class);
            for(SecurityOwnDBUsers user : configObject.getUsers()){
                try{
                    realm.createAccount(user.getUserId(),new String((Files.readAllBytes(Paths.get(user.getPath())))).trim());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            LOGGER.info("Applying configuration...");
        }

    }
    @Override
    public boolean isConfigurationValid(String config) {
        return false;
    }
}

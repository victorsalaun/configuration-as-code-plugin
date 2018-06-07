package org.jenkinsci.plugins.casc.credentials;

import com.cloudbees.plugins.credentials.GlobalCredentialsConfiguration;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
public class CredentialsRootConfigurator extends BaseConfigurator<GlobalCredentialsConfiguration> implements RootElementConfigurator<GlobalCredentialsConfiguration> {

    private final static Logger logger = Logger.getLogger(CredentialsRootConfigurator.class.getName());

    @Override
    public String getName() {
        return "credentials";
    }

    @Override
    public Class<GlobalCredentialsConfiguration> getTarget() {
        return GlobalCredentialsConfiguration.class;
    }

    @Override
    public GlobalCredentialsConfiguration getTargetComponent() {
        return GlobalCredentialsConfiguration.all().get(GlobalCredentialsConfiguration.class);
    }

    @Override
    public GlobalCredentialsConfiguration configure(CNode config) throws ConfiguratorException {
        final GlobalCredentialsConfiguration target = getTargetComponent();
        configure(config.asMapping(), target);
        return target;
    }

    @Override
    public GlobalCredentialsConfiguration test(CNode config) throws ConfiguratorException {
        final GlobalCredentialsConfiguration target = getTargetComponent();
        test(config.asMapping(), target);
        return target;
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute<Jenkins, SystemCredentialsProvider>("system", SystemCredentialsProvider.class)
            .getter( t -> SystemCredentialsProvider.getInstance() )
            .setter( Attribute.NOOP ));
    }

    @CheckForNull
    @Override
    public CNode describe(GlobalCredentialsConfiguration instance) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : describe()) {
            mapping.put(attribute.getName(), attribute.describe(Jenkins.getInstance()));
        }
        return mapping;
    }

}

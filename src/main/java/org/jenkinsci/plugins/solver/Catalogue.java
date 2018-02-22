package org.jenkinsci.plugins.solver;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface Catalogue {

    Plugin findPlugin(String shortname, String version) throws IOException;

    default Plugin findPlugin(PluginSpec spec) throws IOException {
        return findPlugin(spec.shortName, spec.version.toString());
    }

}

package org.jenkinsci.plugins.solver;

import hudson.util.VersionNumber;

import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class RootPlugin extends Plugin {


    public RootPlugin(List<Dependency> dependencies) {
        super("root", "root", new VersionNumber("0"), null, dependencies, null);
    }
}

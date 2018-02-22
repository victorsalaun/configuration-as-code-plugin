package org.jenkinsci.plugins.solver;

import java.io.File;

/**
 * a source for getting a plugin. Might be by downloading from update-center, or any other provisioning strategy
 */
public interface PluginSource {

    File getHPIFile();
}

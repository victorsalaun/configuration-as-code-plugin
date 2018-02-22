package org.jenkinsci.plugins.solver;

public class PluginSpecFormatException extends RuntimeException {
    public PluginSpecFormatException(String message) {
        super(message);
    }

    public PluginSpecFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

package org.jenkinsci.plugins.solver;

import hudson.util.VersionNumber;

import java.util.Objects;

/**
 * Specifies a plugin name and version
 */
public class PluginSpec {

    private static final String LATEST = "latest";

    /**
     * Plugin "short name" used to refer to the plugin in the Jenkins plugin system
     */
    public final String shortName;

    /**
     * Plugin version
     */
    public final VersionNumber version;

    public boolean isLatest() {
        return version.toString().equals(LATEST);
    }

    public PluginSpec(String shortName,
                      VersionNumber version) {
        if (shortName == null || version == null) {
            throw new NullPointerException("WTF!");
        }
        this.shortName = shortName;
        this.version = version;
    }

    @Override
    public String toString() {
        return shortName + ":" + version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        PluginSpec spec = (PluginSpec) o;
        return shortName.equals(spec.shortName) &&
                version.equals(spec.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortName, version);
    }

    public static PluginSpec latest(String shortName) {
        return new PluginSpec(shortName, new VersionNumber(LATEST));
    }

    /**
     * @param shortName for new spec
     * @param version   for new spec
     * @return PluginSpec
     */
    public static PluginSpec fromNameAndVersion(String shortName, VersionNumber version) {
        return new PluginSpec(shortName, version);
    }

    public static PluginSpec fromString(String shortName, String versionString) {
        if (versionString == null || versionString.length() == 0) {
            throw new PluginSpecFormatException("Version component of plugin spec was empty " + shortName + ":" + versionString);
        }
        VersionNumber version;
        try {
            version = new VersionNumber(versionString);
        } catch (Throwable e) {
            throw new PluginSpecFormatException("Could not parse version component of plugin spec " + shortName + ":" + versionString, e);
        }
        return fromNameAndVersion(shortName, version);
    }

    public static PluginSpec fromString(String spec) {
        String[] split = spec.split(":");
        if (split.length != 2) {
            throw new PluginSpecFormatException("Bad PluginSpec format " + spec);
        }
        return fromString(split[0], split[1]);
    }
}

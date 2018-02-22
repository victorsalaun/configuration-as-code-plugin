package org.jenkinsci.plugins.solver;

import hudson.util.VersionNumber;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Definition of a plugin. Subset of what can be found in the manifest file of an HPI archive
 */
public class Plugin implements Comparable<Plugin> {

    public String groupId;

    public String shortName;

    public VersionNumber version;

    public final VersionNumber requiredJenkinsCoreVersion;

    public final List<Dependency> dependencies;

    public final PluginSource source;
    
    private List<String> breaks = new ArrayList<>();

    public Plugin(String groupId,
                  String shortName,
                  VersionNumber version,
                  VersionNumber requiredJenkinsCoreVersion,
                  List<Dependency> dependencies,
                  PluginSource source) {
        this.groupId = groupId;
        this.shortName = shortName;
        this.version = version;
        this.requiredJenkinsCoreVersion = requiredJenkinsCoreVersion;
        this.dependencies = dependencies;
        this.source = source;
    }


    public List<Dependency> getDependencies() {
        return dependencies != null ? dependencies : Collections.EMPTY_LIST;
    }

    public void addBreak(String b) {
        breaks.add(b);
    }

    public List<String> getBreaks() {
        return breaks;
    }

    /*
             *  most code comes from update_center2 HPI class
             */
    public static class BuilderFromManifest {
        final Manifest manifest;

        final PluginSource source;

        public static final VersionNumber CUT_OFF = new VersionNumber("1.395");

        BuilderFromManifest(Manifest manifest, PluginSource source) {
            this.manifest = manifest;
            this.source = source;
        }
        /**
         * Earlier versions of the maven-hpi-plugin put "null" string literal, so we need to treat it as real null.
         */
        private static String fixNull(String v) {
            if("null".equals(v))    return null;
            return v;
        }

        public Attributes getManifestAttributes() {
            return manifest.getMainAttributes();
        }

        public String getRequiredJenkinsVersion() {
            String v = getManifestAttributes().getValue("Jenkins-Version");
            if (v != null) {
                return v;
            }

            v = getManifestAttributes().getValue("Hudson-Version");
            if (fixNull(v) != null) {
                try {
                    VersionNumber n = new VersionNumber(v);
                    if (n.compareTo(CUT_OFF)<=0)
                        return v;   // Hudson <= 1.395 is treated as Jenkins
                    // TODO: Jenkins-Version started appearing from Jenkins 1.401 POM.
                    // so maybe Hudson > 1.400 shouldn't be considered as a Jenkins plugin?
                } catch (IllegalArgumentException e) {
                }
            }

            // Parent versions 1.393 to 1.398 failed to record requiredJenkinsCoreVersion.
            // If value is missing, let's default to 1.398 for now.
            return "1.398";
        }

        public List<Dependency> getDependencies() {
            String deps = getManifestAttributes().getValue("Plugin-Dependencies");
            if (deps == null) return Collections.emptyList();

            List<Dependency> r = new ArrayList<Dependency>();
            for (String token : deps.split(","))
                r.add(Dependency.fromManifestToken(token));
            return r;
        }

        public Plugin build() {
            final Attributes attributes = getManifestAttributes();
            try {
                String version = attributes.getValue("Plugin-Version");
                if (version == null) version = attributes.getValue("Implementation-Version"); // old one
                return new Plugin(
                        attributes.getValue("Group-Id"),
                        attributes.getValue("Short-Name"),
                        new VersionNumber(version),
                        new VersionNumber(getRequiredJenkinsVersion()),
                        getDependencies(),
                        source);
            } catch (Exception e) {
                System.err.println("There's something wrong with MANIFEST");
                try {
                    manifest.write(System.err);
                } catch (IOException ex) {
                }
                e.printStackTrace(System.err);
                return null;
            }
        }
    }

    public static Plugin fromManifest(Manifest manifest, PluginSource source) throws IOException {
        return new BuilderFromManifest(manifest, source).build();
    }

    public static Optional<Plugin> fromJar(File file) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            final Manifest manifest = jarFile.getManifest();
            if (manifest == null) return Optional.empty();
            return Optional.of(fromManifest(manifest, () -> file));
        }
    }

    /*
     *  from update_center2
     */
    public static class Dependency extends PluginSpec {
        public final boolean optional;

        public Dependency(String name, VersionNumber version, boolean optional) {
            super(name, version);
            this.optional = optional;
        }

        public static Dependency fromManifestToken(String token) {
            boolean optional = token.endsWith(OPTIONAL_RESOLUTION);
            if(optional)
                token = token.substring(0, token.length()-OPTIONAL_RESOLUTION.length());

            String[] pieces = token.split(":");
            String name = pieces[0];
            String version = pieces[1];
            return new Dependency(name, new VersionNumber(version), optional);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        private static final String OPTIONAL_RESOLUTION = ";resolution:=optional";
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof Plugin)) {
            return false;
        }
        Plugin p = (Plugin) o;
        return shortName.equals(p.shortName) &&
                version.equals(p.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortName, version);
    }

    @Override
    public int compareTo(Plugin o) {
        final int i = shortName.compareTo(o.shortName);
        if (i != 0) return i;
        return version.compareTo(o.version);
    }

    @Override
    public String toString() {
        return groupId + ":" + shortName + ":" + version;
    }
}

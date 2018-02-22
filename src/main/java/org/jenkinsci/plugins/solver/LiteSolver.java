package org.jenkinsci.plugins.solver;

import hudson.util.VersionNumber;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dependency solver using a naive "lite" iterative algorithm.
 * "Naive" because this guy is far from addressing the actual requirements for <a href="https://en.wikipedia.org/wiki/Dependency_hell">Dependency resoluton</a>.
 * So far it supports <a href="https://www.debian.org/doc/debian-policy/#s-breaks">"Break"</a> but would need to be extended for  Pre-Depends, Recommends, Suggests, Conflicts, Provides, Replaces, Enhances ...
 * Also, without actual metadata in update-center, this code is only a proof of concept.
 *
 *
 * <ol>
 *     <li>starting from a set of required "root" plugins with fixed version</li>
 *     <li>we build a graph of (possibly optional) transitive depedencies</li>
 *     <li>we search for version conflicts. For each conflict found, we keep newest and remove the older from the graph, with all it's transitive dependencies ("orphaned")</li>
 *     <li>we search for compatibility breaks in resulting graph. If one is found, we consider the minimal required version involved by this break as a "phantom" root plugin.</li>
 *     <li>if a break has been found, run another iteration. If none, we have found minimal required plugin set for requested installation</li>
 * </ol>
 *
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LiteSolver {

    private static final int MAXVAR = 1_000_000;

    private final Catalogue catalogue;

    public LiteSolver(Catalogue catalogue) {
        this.catalogue = catalogue;
    }


    @Option(name="-in", usage="Configuration as code yaml file")
    public File in = new File("plugins.yaml");

    @Option(name="-out", usage="plugins.txt result file")
    public File out = new File("plugins.txt");

    public static void main(String[] args) throws Exception {

        final Catalogue catalogue = new CatalogueImpl(new File(".cache"));
        final LiteSolver solver = new LiteSolver(catalogue);
        new CmdLineParser(solver).parseArgument(args);

        solver.solve();
    }

    public void solve() throws Exception {

        List<Plugin.Dependency> dependencies = new ArrayList<>();

        final Map load = (Map) new Yaml().load(new FileReader(in));
        Map<String,Object> map = (Map) load.get("plugins");
        if (map == null) System.exit(0);
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object v = e.getValue();
            dependencies.add(new Plugin.Dependency(e.getKey(), new VersionNumber(String.valueOf(v)), false));
        }

        final List<Plugin> plugins = new ArrayList<>(solve(dependencies));
        Collections.sort(plugins);

        try (Writer w = new FileWriter(out)) {
            for (Plugin plugin : plugins) {
                w.write(plugin.shortName + ':' + plugin.version + '\n');
            }
        }
        System.out.println("plugin list dumped to "  + out.getCanonicalPath());
    }

    private Set<Plugin> solve(List<Plugin.Dependency> want) throws IOException {

        final Plugin root = new RootPlugin(want);

        List<Plugin> phantoms = new ArrayList<>();
        while (true) {

            DirectedAcyclicGraph<Plugin, DependencyEdge> graph = new DirectedAcyclicGraph<>(DependencyEdge.class);
            graph.addVertex(root);
            for (Plugin.Dependency dependency : root.getDependencies()) {
                addPluginDependency(root, dependency, graph);
            }

            for (Plugin phantom : phantoms) {
                addPluginDependency(root, graph, phantom, true);
            }

            // We search the graph for conflicting version of dependencies. When found, the later wins and the graph
            // is updated to reference it and the older version is removed from graph. Then we remove potential orphaned node
            // (transitive dependencies from this older version)
            while (searchVersionConflicts(graph)) {
                removeOrphaned(graph, root);
            }


            // We search the graph for optional dependencies that aren't reachable by a non-optional dependency edge
            // and remove them, including possible orphaned transitive dependencies.
            removeOptionalOnlyDependencies(graph, root);

            // We now search for breaks, and force update to more recent version if required
            final List<Plugin> breaks = searchForBreaks(graph);
            boolean hasBreaks = breaks.size() > 0;
            // We introduce a phantom dependency so next run will force upgrade
            phantoms.addAll(breaks);

            if (!hasBreaks) {
                // Bingo !
                Set<Plugin> result = new HashSet<>(graph.vertexSet());
                result.remove(root);
                return result;
            }
        }
    }

    private List<Plugin> searchForBreaks(DirectedAcyclicGraph<Plugin, DependencyEdge> graph) throws IOException {

        List<Plugin> breaks = new ArrayList<>();

        boolean graphUpdated = false;
        LOOP:
        for (Plugin plugin : graph) {
            for (String b : plugin.getBreaks()) {
                int i = b.indexOf(':');
                String shortname = b.substring(0,i);
                String version = b.substring(i+1);
                final Plugin selected = getPlugin(graph, shortname);
                if (selected == null) {
                    // no worries, we don't have this plugin here
                    continue;
                }
                final VersionNumber v = new VersionNumber(version);
                if (selected.version.isOlderThan(v)) {
                    // Current version will break, we need to upgrade
                    breaks.add(catalogue.findPlugin(shortname, version));
                }
            }
        }
        return breaks;
    }

    private Plugin getPlugin(DirectedAcyclicGraph<Plugin, DependencyEdge> graph, String shortname) {
        for (Plugin plugin : graph) {
            if (plugin.shortName.equals(shortname)) return plugin;
        }
        return null;
    }

    private void removeOptionalOnlyDependencies(DirectedAcyclicGraph<Plugin, DependencyEdge> graph, Plugin root) {
        List<Plugin> optionals = new ArrayList<>();
        for (Plugin plugin : graph) {
            if (plugin.equals(root)) continue;
            boolean optional = true;
            for (DependencyEdge edge : graph.incomingEdgesOf(plugin)) {
                optional &= edge.isOptional();
            }
            if (optional) optionals.add(plugin);
        }
        for (Plugin plugin : optionals) {
            graph.removeVertex(plugin);
            System.out.println("Remove optional (only) dependency "+plugin);
        }
        removeOrphaned(graph, root);
    }

    private boolean searchVersionConflicts(DirectedAcyclicGraph<Plugin, DependencyEdge> graph) {
        for (Plugin plugin : graph) {
            // Search the graph for any dependency for same plugin but distinct version
            for (Plugin o : graph) {
                if (plugin.equals(o)) continue;
                if (plugin.shortName.equals(o.shortName)) {
                    if (plugin.version.isNewerThan(o.version)) {
                        replace(graph, o, plugin);
                    } else {
                        replace(graph, plugin, o);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void removeOrphaned(DirectedAcyclicGraph<Plugin, DependencyEdge> graph, Plugin root) {
        final Set<Plugin> descendants = graph.getDescendants(graph, root);

        final Set<Plugin> all = new HashSet<>(graph.vertexSet());
        all.removeAll(descendants);
        all.remove(root);
        for (Plugin plugin : all) {
            graph.removeVertex(plugin);
        }

    }

    private void replace(DirectedGraph<Plugin, DependencyEdge> graph, Plugin from, Plugin to) {
        final Set<DependencyEdge> incoming = graph.incomingEdgesOf(from);
        for (DependencyEdge edge : incoming) {
            graph.addEdge(edge.getSource(), to, new DependencyEdge(edge.optional));
        }
        graph.removeVertex(from);
    }


    private void addPluginDependency(Plugin plugin, Plugin.Dependency dependency, Graph<Plugin, DependencyEdge> graph) throws IOException {
        Plugin p = catalogue.findPlugin(dependency);
        if (p == null) {
            System.err.println("unknown plugin " + dependency + " required by "+ plugin);
            System.exit(-1);
        }
        addPluginDependency(plugin, graph, p, dependency.optional);
    }

    private void addPluginDependency(Plugin plugin, Graph<Plugin, DependencyEdge> graph, Plugin p, boolean optional) throws IOException {

        graph.addVertex(p);
        graph.addEdge(plugin, p, new DependencyEdge(optional));

        for (Plugin.Dependency transitive : p.getDependencies()) {
            addPluginDependency(p, transitive, graph);
        }
    }

}



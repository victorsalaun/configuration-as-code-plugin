package org.jenkinsci.plugins.solver;

import org.jgrapht.graph.DefaultEdge;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DependencyEdge extends DefaultEdge {


    boolean optional;

    public DependencyEdge(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public Plugin getSource() {
        return (Plugin) super.getSource();
    }

    @Override
    public Plugin getTarget() {
        return (Plugin) super.getTarget();
    }
}

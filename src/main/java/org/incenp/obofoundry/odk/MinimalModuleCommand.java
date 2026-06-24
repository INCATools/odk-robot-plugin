/*
 * ODK ROBOT Plugin
 * Copyright © 2026 Damien Goutte-Gattat
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.incenp.obofoundry.odk;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.util.OWLAxiomVisitorExAdapter;

/**
 * Experimental command to create a “minimal” ontology module.
 * <p>
 * As currently envisioned, a “minimal” module contains only, for any given
 * class <em>C</em>:
 * <ul>
 * <li><code>rdfs:label<code> annotations;
 * <li><code>SubClassOf(C, D)</code> axioms, where <em>D</em> is another class;
 * <li><code>SubClassOf(C, R some D)</code> axioms, where <em>D</em> is another
 * class and <em>R</em> is an object property from an explicitly specified set
 * of properties to preserve;
 * </ul>
 * <p>
 * The module should also contain <code>Transitive(R)</code> axioms for all
 * <em>R</em> properties in the aforementioned set.
 */
public class MinimalModuleCommand extends BasePlugin {

    public MinimalModuleCommand() {
        super("minimal-module", "create a minimal module", "robot minimal-module -i <INPUT> [-p PROPERTY,...]");

        options.addOption("p", "property", true, "properties to preserve in existential restrictions");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        OWLOntology ont = state.getOntology();
        OWLOntologyManager mgr = ont.getOWLOntologyManager();

        Set<IRI> properties = new HashSet<>();
        if ( line.hasOption("property") ) {
            for ( String p : line.getOptionValues("property") ) {
                properties.add(getIRI(p, "property"));
            }
        }

        Set<OWLAxiom> toRemove = new HashSet<>();
        AxiomVisitor visitor = new AxiomVisitor(properties);
        for ( OWLAxiom ax : ont.getAxioms() ) {
            if ( !ax.accept(visitor) ) {
                toRemove.add(ax);
            }
        }
        mgr.removeAxioms(ont, toRemove);
    }
}

class AxiomVisitor extends OWLAxiomVisitorExAdapter<Boolean> {

    private Set<IRI> properties;

    AxiomVisitor(Set<IRI> properties) {
        super(false);
        this.properties = properties;
    }

    @Override
    public Boolean visit(OWLAnnotationAssertionAxiom axiom) {
        // Only keep rdfs:label
        if ( axiom.getProperty().isLabel() ) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visit(OWLSubClassOfAxiom axiom) {
        // Only keep:
        // - SubClassOf(C, D) where both C and D are named classes
        // - SubClassOf(C, R some D) where both C and D are named classes and R is one
        // of the to-be-preserved properties
        if ( !axiom.getSubClass().isNamed() ) {
            return false;
        }
        OWLClassExpression superClass = axiom.getSuperClass();
        if ( superClass.isNamed() ) {
            return true;
        } else {
            // Does not feel worth it to use a OWLClassExpressionVisitor...
            if ( superClass.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM ) {
                OWLObjectSomeValuesFrom osvf = (OWLObjectSomeValuesFrom) superClass;
                if ( properties.contains(osvf.getProperty().getNamedProperty().getIRI())
                        && osvf.getFiller().isNamed() ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Boolean visit(OWLTransitiveObjectPropertyAxiom axiom) {
        // Only keep for the selected properties
        if ( properties.contains(axiom.getProperty().getNamedProperty().getIRI()) ) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visit(OWLDeclarationAxiom axiom) {
        // Only keep for classes and selected object properties
        if ( axiom.getEntity().isOWLObjectProperty() && properties.contains(axiom.getEntity().getIRI()) ) {
            return true;
        }
        if ( axiom.getEntity().isOWLClass() ) {
            return true;
        }
        return false;
    }
}

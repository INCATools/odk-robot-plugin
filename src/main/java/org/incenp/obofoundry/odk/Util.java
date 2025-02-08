/*
 * ODK ROBOT Plugin
 * Copyright © 2025 Damien Goutte-Gattat
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

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Helper methods used throughout the plugin.
 */
public final class Util {

    /**
     * Checks whether a class is dangling.
     * <p>
     * In the context of this plugin, a class is considered to be “dangling” if it
     * has no defining axioms (not counting disjointness axioms) and no annotations.
     * 
     * @param ontology The ontology the class belongs to.
     * @param klass    The class to check.
     * @return {@code true} if the class is dangling, {@code false} otherwise.
     */
    public static boolean isDangling(OWLOntology ontology, OWLClass klass) {
        int nAxioms = 0;
        for ( OWLAxiom ax : ontology.getAxioms(klass, Imports.INCLUDED) ) {
            if ( ax instanceof OWLSubClassOfAxiom ) {
                // Ignore any explicit "SubClassOf owl:Thing" axiom
                if ( !((OWLSubClassOfAxiom) ax).getSuperClass().isTopEntity() ) {
                    nAxioms += 1;
                }
            } else if ( !(ax instanceof OWLDisjointClassesAxiom) ) {
                nAxioms += 1;
            }
        }
        nAxioms += ontology.getAnnotationAssertionAxioms(klass.getIRI()).size();

        return nAxioms == 0;
    }

    /**
     * Checks whether a class is marked as obsolete.
     * 
     * @param ontology The ontology the class belongs to.
     * @param klass    The class to check.
     * @return {@code true} if the class is obsolete, {@code false} otherwise.
     */
    public static boolean isObsolete(OWLOntology ontology, OWLClass klass) {
        for ( OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(klass.getIRI()) ) {
            if ( ax.getProperty().isDeprecated() ) {
                OWLAnnotationValue value = ax.getValue();
                if ( value.isLiteral() ) {
                    OWLLiteral litValue = value.asLiteral().get();
                    if ( litValue.isBoolean() && litValue.getLiteral().equals("true") ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

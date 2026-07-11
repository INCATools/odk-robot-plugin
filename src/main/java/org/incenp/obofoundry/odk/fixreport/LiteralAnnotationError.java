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

package org.incenp.obofoundry.odk.fixreport;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a fixable error that resides in a literal annotation.
 */
public abstract class LiteralAnnotationError implements IFixableError {

    private static final Logger logger = LoggerFactory.getLogger(LiteralAnnotationError.class);

    protected IRI property;
    protected IRI subject;
    protected String value;

    /**
     * Constructs a new instance.
     * 
     * @param property The property of the annotation to fix.
     * @param subject  The entity carrying the annotation to fix.
     * @param value    The value of the annotation. This is the value as reported by
     *                 ROBOT’s <code>report</code> command, i.e. as a string of the
     *                 form <code>VALUE[@LANG]</code>.
     */
    public LiteralAnnotationError(IRI property, IRI subject, String value) {
        this.property = property;
        this.subject = subject;
        this.value = value;
    }

    @Override
    public void fixError(OWLOntology ontology, boolean dubious) {
        OWLOntologyManager mgr = ontology.getOWLOntologyManager();
        OWLDataFactory fac = mgr.getOWLDataFactory();

        for ( OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(subject) ) {
            if ( ax.getProperty().getIRI().equals(property) ) {
                if ( ax.getValue().isLiteral() ) {
                    OWLLiteral oldValue = ax.getValue().asLiteral().get();
                    String textValue = oldValue.getLiteral();
                    if ( oldValue.hasLang() ) {
                        textValue += "@" + oldValue.getLang();
                    }
                    if ( textValue.equals(value) ) {
                        OWLLiteral newValue = fixValue(oldValue, fac, dubious);
                        if ( newValue != null ) {
                            logger.info("Fixing value of {} annotation on {}: {} -> {}", property.getShortForm(),
                                    subject.getShortForm(), oldValue, newValue);

                            OWLAxiom newAx = fac.getOWLAnnotationAssertionAxiom(fac.getOWLAnnotationProperty(property),
                                    subject, newValue);
                            mgr.addAxiom(ontology, newAx.getAnnotatedAxiom(ax.getAnnotations()));
                            mgr.removeAxiom(ontology, ax);
                        }
                    }
                }
            }
        }
    }

    /**
     * Fixes the annotation value.
     * 
     * @param oldValue The literal representing the invalid annotation value.
     * @param factory  A factory that can be used to create the correct value.
     * @param dubious  If <code>true</code>, fix the error even if we are not
     *                 certain it is actually an error.
     * @return The correct value to use to replace the original one, or
     *         <code>null</code> if correction could not be performed for any
     *         reason.
     */
    protected abstract OWLLiteral fixValue(OWLLiteral oldValue, OWLDataFactory factory, boolean dubious);
}

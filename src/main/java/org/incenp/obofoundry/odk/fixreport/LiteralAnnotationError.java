/*
 * ODK ROBOT Plugin
 * Copyright © 2026 Damien Goutte-Gattat
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   (1) Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   (2) Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 *   (3) Neither the name of the copyright holder nor the names its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
    public boolean fixError(OWLOntology ontology, boolean dubious) {
        OWLOntologyManager mgr = ontology.getOWLOntologyManager();
        OWLDataFactory fac = mgr.getOWLDataFactory();
        boolean fixed = false;

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

                            fixed = true;
                        }
                    }
                }
            }
        }

        return fixed;
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

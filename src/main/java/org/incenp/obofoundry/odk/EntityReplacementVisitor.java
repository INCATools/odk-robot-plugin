/*
 * ODK ROBOT Plugin
 * Copyright © 2025 Nico Matentzoglu
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

package org.incenp.obofoundry.odk;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;

/**
 * A visitor that replaces all occurrences of one entity with another in an
 * axiom.
 * <p>
 * This visitor extends {@link OWLObjectDuplicator} to traverse an axiom and
 * replace all occurrences of a specified entity with a replacement entity.
 */
class EntityReplacementVisitor extends OWLObjectDuplicator {

    private final OWLEntity obsolete;
    private final OWLEntity replacement;

    /**
     * Creates a new entity replacement visitor.
     *
     * @param factory     The data factory to use.
     * @param obsolete    The entity to replace.
     * @param replacement The replacement entity.
     */
    public EntityReplacementVisitor(OWLDataFactory factory, OWLEntity obsolete, OWLEntity replacement) {
        super(factory);
        this.obsolete = obsolete;
        this.replacement = replacement;
    }

    @Override
    public void visit(OWLClass cls) {
        if (cls.equals(obsolete)) {
            setLastObject((OWLClass) replacement);
        } else {
            setLastObject(cls);
        }
    }

    @Override
    public void visit(OWLObjectProperty property) {
        if (property.equals(obsolete)) {
            setLastObject((OWLObjectProperty) replacement);
        } else {
            setLastObject(property);
        }
    }

    @Override
    public void visit(OWLDataProperty property) {
        if (property.equals(obsolete)) {
            setLastObject((OWLDataProperty) replacement);
        } else {
            setLastObject(property);
        }
    }

    @Override
    public void visit(OWLNamedIndividual individual) {
        if (individual.equals(obsolete)) {
            setLastObject((OWLNamedIndividual) replacement);
        } else {
            setLastObject(individual);
        }
    }
}

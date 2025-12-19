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

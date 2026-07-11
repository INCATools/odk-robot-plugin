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

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Represents an error (as reported by ROBOT’s <code>report</code> command) that
 * can be automatically fixed.
 */
public interface IFixableError {

    /**
     * Fixes the reported error.
     * 
     * @param ontology The ontology containing the error to fix.
     * @param dubious  If <code>true</code>, fix the error even if we are not
     *                 certain it is actually an error.
     */
    public void fixError(OWLOntology ontology, boolean dubious);
}

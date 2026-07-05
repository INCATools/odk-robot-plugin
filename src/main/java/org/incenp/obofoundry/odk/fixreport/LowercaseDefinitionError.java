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

import org.incenp.obofoundry.odk.Constants;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * Represents the <code>lowercase_definition</code> error>
 * <p>
 * This error is fixed simply by upper-casing the first character of the
 * original value.
 */
public class LowercaseDefinitionError extends LiteralAnnotationError {

    public LowercaseDefinitionError(IRI subject, String value) {
        super(Constants.IAO_DEFINITION, subject, value);
    }

    @Override
    protected OWLLiteral fixValue(OWLLiteral oldValue, OWLDataFactory factory) {
        String oldValueString = oldValue.getLiteral();
        String newValueString = Character.toUpperCase(oldValueString.charAt(0)) + oldValueString.substring(1);
        return factory.getOWLLiteral(newValueString, oldValue.getLang());
    }

}

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
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Represents the <code>missing_obsolete_label</code> error.
 * <p>
 * This error is fixed simply by appending <code>"obsolete "</code> in front of
 * the original value, but only if the original value either has no language
 * tag, or has an English language tag.
 */
public class MissingObsoleteLabelError extends LiteralAnnotationError {

    public MissingObsoleteLabelError(IRI subject, String value) {
        super(OWLRDFVocabulary.RDFS_LABEL.getIRI(), subject, value);
    }

    @Override
    protected OWLLiteral fixValue(OWLLiteral oldValue, OWLDataFactory factory, boolean dubious) {
        String lang = oldValue.getLang();
        if ( !(lang.isEmpty() || lang.startsWith("en")) ) {
            // Do NOT append an "obsolete " prefix to a non-English label
            return null;
        }
        return factory.getOWLLiteral("obsolete " + oldValue.getLiteral(), lang);
    }

}

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

import org.incenp.obofoundry.odk.Constants;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * Represents the <code>lowercase_definition</code> error.
 * <p>
 * This error is fixed simply by upper-casing the first character of the
 * original value. However we avoid doing that by default if the initial
 * lowercase character is followed by an uppercase character, as this could
 * indicate the initial character is <em>purposefully</em> lowercase.
 */
public class LowercaseDefinitionError extends LiteralAnnotationError {

    public LowercaseDefinitionError(IRI subject, String value) {
        super(Constants.IAO_DEFINITION, subject, value);
    }

    @Override
    protected OWLLiteral fixValue(OWLLiteral oldValue, OWLDataFactory factory, boolean dubious) {
        String oldValueString = oldValue.getLiteral();
        if ( oldValueString.length() > 1 && Character.isUpperCase(oldValueString.charAt(1)) && !dubious ) {
            return null;
        }
        String newValueString = Character.toUpperCase(oldValueString.charAt(0)) + oldValueString.substring(1);
        return factory.getOWLLiteral(newValueString, oldValue.getLang());
    }

}

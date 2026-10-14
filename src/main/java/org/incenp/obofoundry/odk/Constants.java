/*
 * ODK ROBOT Plugin
 * Copyright © 2025 Damien Goutte-Gattat
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

import org.semanticweb.owlapi.model.IRI;

/**
 * Constant values used throughout the plugin.
 */
public class Constants {
    public static final String OIO_PREFIX = "http://www.geneontology.org/formats/oboInOwl#";

    public static final IRI DC_SOURCE = IRI.create("http://purl.org/dc/elements/1.1/source");

    public static final IRI IN_SUBSET = IRI.create(OIO_PREFIX + "inSubset");

    public static final IRI HAS_SYNONYM_TYPE = IRI.create(OIO_PREFIX + "hasSynonymType");

    public static final IRI SUBSET_PROPERTY = IRI.create(OIO_PREFIX + "SubsetProperty");

    public static final IRI SYNONYM_TYPE_PROPERTY = IRI.create(OIO_PREFIX + "SynonymTypeProperty");

    public static final IRI COB_IRI = IRI.create("http://purl.obolibrary.org/obo/cob.owl");

    public static final IRI PREFERRED_ROOT_PROPERTY = IRI.create("http://purl.obolibrary.org/obo/IAO_0000700");

    public static final IRI RDFS_LABEL = IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

    public static final IRI RDFS_COMMENT = IRI.create("http://www.w3.org/2000/01/rdf-schema#comment");

    public static final IRI IAO_DEFINITION = IRI.create("http://purl.obolibrary.org/obo/IAO_0000115");

    public static final IRI HAS_EXACT_SYNONYM = IRI.create(OIO_PREFIX + "hasExactSynonym");

    public static final IRI HAS_DBXREF = IRI.create(OIO_PREFIX + "hasDbXref");

    public static final IRI TERM_REPLACED_BY = IRI.create("http://purl.obolibrary.org/obo/IAO_0100001");

    public static final IRI ORCIDIO_IRI = IRI.create("https://w3id.org/orcidio/orcidio.owl");
}

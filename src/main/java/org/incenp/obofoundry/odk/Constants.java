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

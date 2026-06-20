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

package org.incenp.obofoundry.odk;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

public class ExtractOrcidCommand extends BasePlugin {

    private final static IRI ORCIDIO = IRI.create("https://w3id.org/orcidio/orcidio.owl");

    public ExtractOrcidCommand() {
        super("extract-orcids", "extract ORCIDs referenced in the ontology",
                "robot extract-orcids -i <INPUT> [--orcid-file <FILE>]");

        options.addOption(null, "orcid-file", true, "extract ORCIDs from the specified ontology");
        options.addOption(null, "orcid-iri", true, "extract ORCIDs from the specified ontology IRI");
        options.addOption(null, "property", true,
                "extract ORCIDs referenced in annotations with the specified property");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        OWLOntology source = state.getOntology();
        OWLOntologyManager mgr = OWLManager.createConcurrentOWLOntologyManager();
        OWLDataFactory factory = mgr.getOWLDataFactory();

        Set<IRI> properties = new HashSet<>();
        if ( line.hasOption("property") ) {
            for ( String p : line.getOptionValues("property") ) {
                properties.add(getIRI(p, "property"));
            }
        }

        // Collect all IRIs used in annotation with the target properties
        Set<IRI> refs = new HashSet<>();
        for ( OWLAnnotation annot : source.getAnnotations() ) {
            processAnnotation(annot, refs, properties);
        }
        for ( OWLAxiom ax : source.getAxioms(Imports.INCLUDED) ) {
            if ( ax instanceof OWLAnnotationAssertionAxiom ) {
                processAnnotation(((OWLAnnotationAssertionAxiom) ax).getAnnotation(), refs, properties);
            }
            for ( OWLAnnotation annot : ax.getAnnotations() ) {
                processAnnotation(annot, refs, properties);
            }
        }

        // Get the referenced ORCID individuals
        OWLOntology orcidOnt = null;
        if ( line.hasOption("orcid-file") ) {
            orcidOnt = ioHelper.loadOntology(line.getOptionValue("orcid-file"));
        } else if ( line.hasOption("orcid-iri") ) {
            orcidOnt = ioHelper.loadOntology(ioHelper.createIRI(line.getOptionValue("orcid-iri")));
        } else {
            orcidOnt = ioHelper.loadOntology(ORCIDIO);
        }
        Set<OWLAxiom> axioms = new HashSet<>();
        for ( IRI ref : refs ) {
            if ( orcidOnt.containsIndividualInSignature(ref) ) {
                axioms.addAll(orcidOnt.getAxioms(factory.getOWLNamedIndividual(ref), Imports.INCLUDED));
                axioms.addAll(orcidOnt.getAnnotationAssertionAxioms(ref));
            }
        }

        // Save the result
        OWLOntology output = mgr.createOntology();
        mgr.addAxioms(output, axioms);
        state.setOntology(output);
    }

    // Given an annotation, check if its property is one of the properties we are
    // looking for; if it is (or if we are accepting any annotation property) and
    // the annotation value is an IRI, add the value to the refs set.
    private void processAnnotation(OWLAnnotation annotation, Set<IRI> refs, Set<IRI> properties) {
        if ( properties.isEmpty() || properties.contains(annotation.getProperty().getIRI()) ) {
            if ( annotation.getValue().isIRI() ) {
                refs.add(annotation.getValue().asIRI().get());
            }
        }
    }

}

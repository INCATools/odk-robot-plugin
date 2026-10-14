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

/**
 * A command to extract ORCIDIO individuals referenced within an ontology.
 * <p>
 * Given an input ontology, this command looks for annotations that reference
 * ORCID identifiers, then create an output ontology containing the named
 * individuals from <a href="https://github.com/choyt/orcidio">ORCIDIO</a> that
 * represent the referenced persons.
 */
public class ExtractOrcidCommand extends BasePlugin {

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
            orcidOnt = ioHelper.loadOntology(Constants.ORCIDIO_IRI);
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

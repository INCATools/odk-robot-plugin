/*
 * ODK ROBOT Plugin
 * Copyright © 2025 Nico Matentzoglu
 * This Command was strongly inspired by https://github.com/owlcollab/owltools/blob/master/OWLTools-Runner/src/main/java/owltools/cli/CommandRunner.java
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command to obsolete an entity and replace all its usages with a replacement
 * entity.
 * <p>
 * This command takes an entity to obsolete and a replacement entity. It will:
 * <ul>
 * <li>Replace all usages of the obsolete entity with the replacement entity
 * throughout the ontology
 * <li>Remove the label, comment, and definition from the obsolete entity
 * <li>Mark the obsolete entity as deprecated
 * <li>Add a new label prefixed with "obsolete" to the obsolete entity
 * <li>Add the original label of the obsolete entity as an exact synonym on the
 * replacement entity (with a dbxref annotation pointing to the obsolete entity)
 * <li>Add a "term replaced by" annotation on the obsolete entity pointing to the
 * replacement entity
 * </ul>
 */
public class ObsoleteReplaceCommand extends BasePlugin {

    private static final Logger logger = LoggerFactory.getLogger(ObsoleteReplaceCommand.class);

    public ObsoleteReplaceCommand() {
        super("obsolete-replace", "obsolete an entity and replace it with another",
                "robot obsolete-replace --obsolete TERM --replacement TERM");

        options.addOption(null, "obsolete", true, "entity to obsolete (CURIE or IRI)");
        options.addOption(null, "replacement", true, "replacement entity (CURIE or IRI)");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        if (!line.hasOption("obsolete") || !line.hasOption("replacement")) {
            throw new IllegalArgumentException("Both --obsolete and --replacement options are required");
        }

        IRI obsoleteIRI = getIRI(line.getOptionValue("obsolete"), "obsolete");
        IRI replacementIRI = getIRI(line.getOptionValue("replacement"), "replacement");

        OWLOntology ontology = state.getOntology();
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLEntity obsoleteEntity = getEntity(ontology, obsoleteIRI);
        if (obsoleteEntity == null) {
            throw new IllegalArgumentException("Entity not found: " + obsoleteIRI);
        }

        OWLEntity replacementEntity = getEntity(ontology, replacementIRI);
        if (replacementEntity == null) {
            throw new IllegalArgumentException("Replacement entity not found: " + replacementIRI);
        }

        String originalLabel = getLabel(ontology, obsoleteIRI);

        Set<OWLAxiom> axiomsToRemove = new HashSet<>();
        for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(obsoleteIRI)) {
            IRI propertyIRI = axiom.getProperty().getIRI();
            if (propertyIRI.equals(Constants.RDFS_LABEL) || propertyIRI.equals(Constants.RDFS_COMMENT)
                    || propertyIRI.equals(Constants.IAO_DEFINITION)) {
                axiomsToRemove.add(axiom);
            }
        }

        logger.info("Removing {} annotation axioms from obsolete entity", axiomsToRemove.size());
        manager.removeAxioms(ontology, axiomsToRemove);

        replaceEntity(ontology, obsoleteEntity, replacementEntity);

        Set<OWLAxiom> axiomsToAdd = new HashSet<>();
        axiomsToAdd.add(factory.getOWLDeclarationAxiom(obsoleteEntity));
        axiomsToAdd.add(factory.getOWLAnnotationAssertionAxiom(factory.getOWLDeprecated(), obsoleteIRI,
                factory.getOWLLiteral(true)));

        String newLabel = "obsolete " + (originalLabel != null ? originalLabel : getLocalName(obsoleteIRI));
        axiomsToAdd.add(factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(), obsoleteIRI,
                factory.getOWLLiteral(newLabel)));

        if (originalLabel != null) {
            OWLAnnotationProperty hasExactSynonym = factory.getOWLAnnotationProperty(Constants.HAS_EXACT_SYNONYM);
            OWLAnnotationProperty hasDbXref = factory.getOWLAnnotationProperty(Constants.HAS_DBXREF);
            OWLAnnotation dbxrefAnnotation = factory.getOWLAnnotation(hasDbXref,
                    factory.getOWLLiteral(getShortForm(obsoleteIRI)));
            Set<OWLAnnotation> synonymAnnotations = Collections.singleton(dbxrefAnnotation);
            axiomsToAdd.add(factory.getOWLAnnotationAssertionAxiom(hasExactSynonym, replacementIRI,
                    factory.getOWLLiteral(originalLabel), synonymAnnotations));
        }

        OWLAnnotationProperty termReplacedBy = factory.getOWLAnnotationProperty(Constants.TERM_REPLACED_BY);
        axiomsToAdd.add(factory.getOWLAnnotationAssertionAxiom(termReplacedBy, obsoleteIRI,
                factory.getOWLLiteral(getShortForm(replacementIRI))));

        logger.info("Adding {} new axioms to obsolete entity and replacement", axiomsToAdd.size());
        manager.addAxioms(ontology, axiomsToAdd);
    }

    private OWLEntity getEntity(OWLOntology ontology, IRI iri) {
        for (OWLEntity entity : ontology.getSignature()) {
            if (entity.getIRI().equals(iri)) {
                return entity;
            }
        }
        return null;
    }

    private String getLabel(OWLOntology ontology, IRI entityIRI) {
        for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(entityIRI)) {
            if (axiom.getProperty().isLabel() && axiom.getValue().isLiteral()) {
                return axiom.getValue().asLiteral().get().getLiteral();
            }
        }
        return null;
    }

    private void replaceEntity(OWLOntology ontology, OWLEntity obsolete, OWLEntity replacement) {
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        Set<OWLAxiom> axiomsToRemove = new HashSet<>();
        Set<OWLAxiom> axiomsToAdd = new HashSet<>();

        for (OWLAxiom axiom : ontology.getAxioms()) {
            if (axiom.getSignature().contains(obsolete)) {
                axiomsToRemove.add(axiom);
                OWLAxiom replacedAxiom = replaceInAxiom(axiom, factory, obsolete, replacement);
                axiomsToAdd.add(replacedAxiom);
            }
        }

        logger.info("Replacing {} axioms containing the obsolete entity", axiomsToRemove.size());
        manager.removeAxioms(ontology, axiomsToRemove);
        manager.addAxioms(ontology, axiomsToAdd);
    }

    private OWLAxiom replaceInAxiom(OWLAxiom axiom, OWLDataFactory factory, OWLEntity obsolete,
            OWLEntity replacement) {
        EntityReplacementVisitor visitor = new EntityReplacementVisitor(factory, obsolete, replacement);
        axiom.accept(visitor);
        return visitor.duplicateObject(axiom);
    }

    private String getShortForm(IRI iri) {
        String iriString = iri.toString();
        if (iriString.contains("#")) {
            return iriString.substring(iriString.lastIndexOf('#') + 1);
        } else if (iriString.contains("/")) {
            return iriString.substring(iriString.lastIndexOf('/') + 1);
        }
        return iriString;
    }

    private String getLocalName(IRI iri) {
        String shortForm = getShortForm(iri);
        if (shortForm.contains("_")) {
            return shortForm.replace('_', ':');
        }
        return shortForm;
    }
}

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * A command to perform various “normalisation” operations on an ontology.
 * <p>
 * Available normalization operations currently include:
 * <ul>
 * <li>injecting <em>SubAnnotationPropertyOf</em> axioms to ensure that IRIs
 * representing subsets (used as the value of {@code oboInOwl#inSubset}
 * annotations) are subproperties of {@code oboInOwl#SubsetProperty};
 * <li>likewise, injecting <em>SubAnnotationPropertyOf</em> axioms to ensure
 * that IRIs representing synonym types (used as the value of
 * {@code oboInOwl#hasSynonymType} annotations) are subproperties of
 * {@code oboInOwl#SynonymTypeProperty};
 * <li>merging logically equivalent axioms, that differ only by their annotation
 * sets;
 * <li>injecting a {@code dc:source} ontology annotation derived from the
 * version IRI.
 * </ul>
 */
public class NormalizeCommand extends BasePlugin {

    private Set<String> basePrefixes = new HashSet<>();

    public NormalizeCommand() {
        super("normalize", "normalize an ontology",
                "robot normalize [--subset-decls] [--synonym-decls] [--merge-axioms]");

        options.addOption(null, "subset-decls", true, "if true, inject declarations for subset properties");
        options.addOption(null, "synonym-decls", true, "if true, inject declarations for synonym properties");
        options.addOption(null, "base-iri", true, "inject declaration for properties in the indicated namespace(s)");

        options.addOption(null, "merge-axioms", true, "if true, merge logically equivalent axioms");

        options.addOption(null, "add-source", true,
                "if true, add a dc:source annotation from the ontology version IRI");

        options.addOption("a", "all", false, "perform all normalization operations available");

        basePrefixes.add("http://purl.obolibrary.org/obo/");
        basePrefixes.add("http://www.ebi.ac.uk/efo/");
        basePrefixes.add("http://w3id/org/biolink/");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        boolean defValue = line.hasOption("all");
        boolean doMergeAxioms = CommandLineHelper.getBooleanValue(line, "merge-axioms", defValue);
        boolean doSubsetDecls = CommandLineHelper.getBooleanValue(line, "subset-decls", defValue);
        boolean doSynonymDecls = CommandLineHelper.getBooleanValue(line, "synonym-decls", defValue);
        boolean doInjectSource = CommandLineHelper.getBooleanValue(line, "add-source", false);

        OWLOntology ontology = state.getOntology();

        if ( doMergeAxioms ) {
            NormalizeCommand.mergeAxioms(ontology);
        }

        if ( doSubsetDecls || doSynonymDecls ) {
            NormalizeCommand.injectDeclarations(ontology, basePrefixes, doSubsetDecls, doSynonymDecls);
        }

        if ( doInjectSource ) {
            IRI versionIRI = state.getOntology().getOntologyID().getVersionIRI().orNull();
            if ( versionIRI != null ) {
                OWLOntologyManager mgr = ontology.getOWLOntologyManager();
                OWLDataFactory fac = mgr.getOWLDataFactory();
                AddOntologyAnnotation change = new AddOntologyAnnotation(ontology,
                        fac.getOWLAnnotation(fac.getOWLAnnotationProperty(Constants.DC_SOURCE), versionIRI));
                mgr.applyChange(change);
            }
        }
    }

    /**
     * Merges all logically equivalent axioms in the given ontology. Logically
     * equivalent axioms, in the context of this command, refers to axioms that
     * differ only by their annotations.
     * 
     * @param ontology The ontology whose logically equivalent axioms are to be
     *                 merged. Axioms from the imports closure are <em>not</em>
     *                 processed.
     */
    public static void mergeAxioms(OWLOntology ontology) {
        OWLOntologyManager mgr = ontology.getOWLOntologyManager();
        Set<OWLAxiom> origAxioms = ontology.getAxioms(Imports.EXCLUDED);
        Map<OWLAxiom, Set<OWLAnnotation>> annotsMap = new HashMap<>();

        for ( OWLAxiom ax : origAxioms ) {
            annotsMap.computeIfAbsent(ax.getAxiomWithoutAnnotations(), k -> new HashSet<>())
                    .addAll(ax.getAnnotations());
        }

        Set<OWLAxiom> mergedAxioms = new HashSet<>();
        for ( Map.Entry<OWLAxiom, Set<OWLAnnotation>> entry : annotsMap.entrySet() ) {
            mergedAxioms.add(entry.getKey().getAnnotatedAxiom(entry.getValue()));
        }

        mgr.removeAxioms(ontology, origAxioms);
        mgr.addAxioms(ontology, mergedAxioms);
    }

    /**
     * Inject <em>SubAnnotationPropertyOf</em> axioms for IRIs representing subsets
     * or synonyms.
     * 
     * @param ontology    The ontology in which axioms should be injected. When
     *                    collecting the IRIs used as subsets or synonym types,
     *                    annotation axioms from the imports closure are
     *                    <em>not</em> taken into account.
     * @param prefixes    A set of IRI prefixes; axioms will only be injected for
     *                    IRIs that start with one of the given prefixes. May be
     *                    {@code null} to force injecting axioms for all IRIs
     *                    regardless of their namespace.
     * @param forSubsets  If {@code true}, inject axioms to declare subset
     *                    properties.
     * @param forSynonyms If {@code true}, inject axioms to declare synonym type
     *                    properties.
     */
    public static void injectDeclarations(OWLOntology ontology, Set<String> prefixes, boolean forSubsets,
            boolean forSynonyms) {
        Set<String> subsets = new HashSet<>();
        Set<String> synonyms = new HashSet<>();

        for ( OWLAxiom ax : ontology.getAxioms(Imports.EXCLUDED) ) {
            if ( ax instanceof OWLAnnotationAssertionAxiom ) {
                collectPropertyIRI(((OWLAnnotationAssertionAxiom) ax).getAnnotation(), prefixes, subsets, synonyms);
            }
            for ( OWLAnnotation annot : ax.getAnnotations() ) {
                collectPropertyIRI(annot, prefixes, subsets, synonyms);
            }
        }

        Set<OWLAxiom> newAxioms = new HashSet<>();
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        if ( forSubsets ) {
            OWLAnnotationProperty parent = factory.getOWLAnnotationProperty(Constants.SUBSET_PROPERTY);
            for ( String subset : subsets ) {
                newAxioms.add(factory.getOWLSubAnnotationPropertyOfAxiom(
                        factory.getOWLAnnotationProperty(IRI.create(subset)), parent));
            }
        }
        if ( forSynonyms ) {
            OWLAnnotationProperty parent = factory.getOWLAnnotationProperty(Constants.SYNONYM_TYPE_PROPERTY);
            for ( String synonym : synonyms ) {
                newAxioms.add(factory.getOWLSubAnnotationPropertyOfAxiom(
                        factory.getOWLAnnotationProperty(IRI.create(synonym)), parent));
            }
        }

        if ( !newAxioms.isEmpty() ) {
            ontology.getOWLOntologyManager().addAxioms(ontology, newAxioms);
        }
    }

    /*
     * Helper method for injectDeclarations; given an annotation, checks if it
     * declares a subset or a synonym type and collects its value in the appropriate
     * set.
     */
    private static void collectPropertyIRI(OWLAnnotation annotation, Set<String> prefixes, Set<String> subsets,
            Set<String> synonyms) {
        if ( annotation.getValue().isIRI() ) {
            IRI propIRI = annotation.getProperty().getIRI();
            if ( propIRI.equals(Constants.IN_SUBSET) ) {
                collectIRI(prefixes, subsets, annotation.getValue().asIRI().get().toString());
            } else if ( propIRI.equals(Constants.HAS_SYNONYM_TYPE) ) {
                collectIRI(prefixes, synonyms, annotation.getValue().asIRI().get().toString());
            }
        }
    }

    /*
     * Helper method for injectDeclarations; checks if the value starts with one of
     * the given prefixes, and if so adds it to the collection.
     */
    private static void collectIRI(Set<String> prefixes, Set<String> collection, String value) {
        if ( prefixes == null ) {
            collection.add(value);
        } else {
            for ( String base : prefixes ) {
                if ( value.startsWith(base) ) {
                    collection.add(value);
                    break;
                }
            }
        }
    }
}

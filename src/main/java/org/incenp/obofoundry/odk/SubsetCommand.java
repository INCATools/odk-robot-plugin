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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.obolibrary.robot.QuotedEntityChecker;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * A command to create an ontology subset.
 * <p>
 * That command is intended to replace both the
 * <code>--extract-ontology-subset --fill-gaps</code> and the
 * <code>--reasoner-query --make-ontology-from-results</code> commands of
 * OWLTools. As such, it supplements the <code>extract -m subset</code> command
 * of the standard distribution of ROBOT, which can be used to replace the
 * <code>--extract-ontology-subset</code> OWLTools command (note the absence of
 * <code>--fill-gaps</code>!).
 */
public class SubsetCommand extends BasePlugin {

    private static final Logger logger = LoggerFactory.getLogger(SubsetCommand.class);

    public SubsetCommand() {
        super("subset", "extract an ontology subset",
                "robot subset [--query DL-QUERY | --subset TERM | --term TERM-FILE]");

        options.addOption("r", "reasoner", true, "reasoner to use");
        options.addOption("c", "collapse-imports-closure", true,
                "if true (default), include axioms from imported modules");

        options.addOption("q", "query", true, "include the results of the given DL query in the subset");
        options.addOption("a", "ancestors", true, "if true, include ancestors of classes retrieved by the DL query");
        options.addOption("t", "term", true, "include the given class in the subset");
        options.addOption("T", "term-file", true, "include the classes listed in the given file in the subset");
        options.addOption("s", "subset", true, "include classes tagged with the specified subset property");

        options.addOption("f", "fill-gaps", true, "if true, fill gaps to closure");
        options.addOption(null, "no-dangling", true, "if true (default), exclude dangling classes when filling gaps");
        options.addOption(null, "follow-property", true,
                "when filling gaps, only follow relations that use the given property");
        options.addOption(null, "follow-in", true, "when filling gaps, only include classes in the given prefix");
        options.addOption(null, "not-follow-in", true, "when filling gaps, exclude classes in the given prefix");

        options.addOption(null, "write-to", true, "write the subset to the specified file");
        options.addOption(null, "ontology-iri", true, "set the ontology IRI of the subset");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        OWLOntology ontology = state.getOntology();
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLReasoner reasoner = CommandLineHelper.getReasonerFactory(line).createNonBufferingReasoner(ontology);
        boolean useImports = CommandLineHelper.getBooleanValue(line, "collapse-imports-closure", true);

        // Setting up the extractor
        SubsetExtractor extractor = new SubsetExtractor(ontology, reasoner);
        extractor.setFillGaps(CommandLineHelper.getBooleanValue(line, "fill-gaps", false));
        extractor.setExcludeDangling(CommandLineHelper.getBooleanValue(line, "no-dangling", true));
        extractor.includeImports(useImports);
        if ( line.hasOption("follow-property") ) {
            for ( String property : line.getOptionValues("follow-property") ) {
                extractor.followProperty(getIRI(property, "follow-property"));
            }
        }
        if ( line.hasOption("follow-in") ) {
            for ( String prefix : line.getOptionValues("follow-in") ) {
                extractor.includePrefix(prefix);
            }
        }
        if ( line.hasOption("not-follow-in") ) {
            for ( String prefix : line.getOptionValues("not-follow-in") ) {
                extractor.excludePrefix(prefix);
            }
        }

        // Setting up the initial subset
        Set<OWLClass> subset = new HashSet<>();

        // 1. From a DL query
        if ( line.hasOption("query") ) {
            QuotedEntityChecker checker = new QuotedEntityChecker();
            checker.addProperty(factory.getRDFSLabel());
            checker.addProvider(new CURIEShortFormProvider(ioHelper.getPrefixes()));
            checker.addAll(ontology);
            ManchesterOWLSyntaxClassExpressionParser p = new ManchesterOWLSyntaxClassExpressionParser(factory, checker);
            boolean withAncestors = line.getOptionValue("ancestors", "false").equals("true");

            for ( String query : line.getOptionValues("query") ) {
                OWLClassExpression expr = p.parse(query);
                if ( expr.isNamed() ) {
                    subset.add(expr.asOWLClass());
                    logger.debug("Adding queried class {}", expr.asOWLClass().getIRI());
                }
                addToSubset(subset, reasoner.getSubClasses(expr, false).getFlattened(), "Adding subclass {}");
                addToSubset(subset, reasoner.getEquivalentClasses(expr).getEntities(), "Adding equivalent class {}");
                if ( withAncestors ) {
                    addToSubset(subset, reasoner.getSuperClasses(expr, false).getFlattened(), "Adding superclass {}");
                }
            }
        }

        // 2. From the name or IRI of a subset defined in the ontology
        if ( line.hasOption("subset") ) {
            for ( String subsetName : line.getOptionValues("subset") ) {
                IRI subsetIRI = ioHelper.createIRI(subsetName);
                if ( subsetIRI != null ) {
                    addToSubset(subset, extractor.getSubset(subsetIRI), "Adding tagged class {}");
                } else {
                    addToSubset(subset, extractor.getSubset(subsetName), "Adding tagged class {}");
                }
            }
        }

        // 3. From an explicit list of terms
        Set<IRI> terms = new HashSet<>();
        if ( line.hasOption("term") ) {
            for ( String term : line.getOptionValues("term") ) {
                terms.add(getIRI(term, "term"));
            }
        }
        if ( line.hasOption("term-file") ) {
            for ( String termFile : line.getOptionValues("term-file") ) {
                terms.addAll(readFileAsIRIs(termFile));
            }
        }
        for ( IRI term : terms ) {
            if ( ontology.containsClassInSignature(term, useImports ? Imports.INCLUDED : Imports.EXCLUDED) ) {
                subset.add(factory.getOWLClass(term));
                logger.debug("Adding selected class {}", term);
            }
        }

        // Actual extraction
        logger.info("Creating ontology from initial subset of {} classes", subset.size());
        OWLOntology subsetOntology = extractor.makeSubset(subset);
        if ( line.hasOption("ontology-iri") ) {
            Optional<IRI> ontologyIRI = Optional.of(getIRI(line.getOptionValue("ontology-iri"), "ontology-iri"));
            OWLOntologyID id = new OWLOntologyID(ontologyIRI, Optional.absent());
            SetOntologyID change = new SetOntologyID(subsetOntology, id);
            subsetOntology.getOWLOntologyManager().applyChange(change);
        }

        // Output
        if ( line.hasOption("write-to") ) {
            ioHelper.saveOntology(subsetOntology, line.getOptionValue("write-to"));
        } else {
            state.setOntology(subsetOntology);
        }
    }

    private void addToSubset(Set<OWLClass> subset, Set<OWLClass> additions, String msg) {
        for ( OWLClass add : additions ) {
            if ( !add.isTopEntity() && !add.isBottomEntity() ) {
                subset.add(add);
                logger.debug(msg, add.getIRI());
            }
        }
    }
}

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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.obolibrary.robot.MergeOperation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command to check the alignment of an ontology against another, upper-level
 * ontology. The ontology is said to be “aligned” if all its classes are
 * subclasses of one of the upper ontology’s classes.
 */
public class ValidateCommand extends BasePlugin {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    private Set<String> basePrefixes = new HashSet<>();

    public ValidateCommand() {
        super("validate", "validate alignment with an upper ontology",
                "robot validate [--upper-ontology[-iri] ONT] [--report-output FILE]");

        options.addOption("u", "upper-ontology", true, "load the upper ontology from the specified file");
        options.addOption("U", "upper-ontology-iri", true, "load the upper ontology from the specified IRI");

        options.addOption("b", "base-iri", true, "only check classes in the specified namespace(s)");
        options.addOption("d", "ignore-dangling", true, "if true, ignore dangling classes");

        options.addOption("r", "reasoner", true, "the reasoner to use");
        options.addOption("O", "report-output", true, "write report to the specified file");
        options.addOption("x", "fail", true, "if true (default), fail if the ontology is misaligned");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        boolean ignoreDangling = CommandLineHelper.getBooleanValue(line, "ignore-dangling", false);
        boolean failOnError = CommandLineHelper.getBooleanValue(line, "fail", true);
        if ( line.hasOption("base-iri") ) {
            for ( String iri : line.getOptionValues("base-iri") ) {
                basePrefixes.add(iri);
            }
        }

        OWLOntology upperOntology = null;
        if ( line.hasOption("upper-ontology") ) {
            upperOntology = ioHelper.loadOntology(line.getOptionValue("upper-ontology"), true);
        } else if ( line.hasOption("upper-ontology-iri") ) {
            upperOntology = ioHelper
                    .loadOntology(getIRI(line.getOptionValue("upper-ontology-iri"), "upper-ontology-iri"));
        } else {
            logger.warn("No upper ontology specified, assuming COB is meant");
            upperOntology = ioHelper.loadOntology(Constants.COB_IRI);
        }

        // The classes we need to check alignment against
        Set<OWLClass> upperClasses = upperOntology.getClassesInSignature(Imports.INCLUDED);
        upperClasses.remove(upperOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing());

        // We merge the current ontology into the upper ontology rather than the other
        // way around, so that the current ontology remains unchanged and can be used
        // for further operations downstream in the ROBOT pipeline.
        MergeOperation.mergeInto(state.getOntology(), upperOntology, true, true);
        OWLReasoner reasoner = CommandLineHelper.getReasonerFactory(line).createReasoner(upperOntology);

        Set<OWLClass> unalignedClasses = new HashSet<>();
        for ( OWLClass klass : upperOntology.getClassesInSignature(Imports.INCLUDED) ) {
            if ( !klass.isTopEntity() && !upperClasses.contains(klass) && isInBase(klass.getIRI().toString()) ) {
                if ( ignoreDangling && Util.isDangling(upperOntology, klass) ) {
                    continue;
                }
                if ( Util.isObsolete(upperOntology, klass) ) {
                    continue;
                }

                Set<OWLClass> ancestors = reasoner.getSuperClasses(klass, false).getFlattened();
                boolean aligned = false;
                for ( OWLClass upperClass : upperClasses ) {
                    if ( ancestors.contains(upperClass) ) {
                        aligned = true;
                        break;
                    }
                }
                if ( !aligned ) {
                    // Report only top-level classes (whose only parent is owl:Thing)
                    if ( ancestors.size() == 1 ) {
                        unalignedClasses.add(klass);
                    }
                }
            }
        }

        if ( line.hasOption("report-output") ) {
            // If a report has been requested, we always produce it, even if no unaligned
            // classes were found
            BufferedWriter writer = new BufferedWriter(new FileWriter(line.getOptionValue("report-output")));
            List<String> unalignedIRIs = new ArrayList<>();
            for ( OWLClass unalignedClass : unalignedClasses ) {
                unalignedIRIs.add(unalignedClass.getIRI().toString());
            }
            unalignedIRIs.sort((a, b) -> a.compareTo(b));
            for ( String iri : unalignedIRIs ) {
                writer.write(iri);
                writer.write('\n');
            }
            writer.close();
        }

        if ( !unalignedClasses.isEmpty() ) {
            logger.error("Ontology contains {} top-level unaligned class(es)", unalignedClasses.size());
            if ( failOnError ) {
                System.exit(1);
            }
        }
    }

    private boolean isInBase(String iri) {
        for ( String base : basePrefixes ) {
            if ( iri.startsWith(base) ) {
                return true;
            }
        }
        return basePrefixes.isEmpty();
    }
}

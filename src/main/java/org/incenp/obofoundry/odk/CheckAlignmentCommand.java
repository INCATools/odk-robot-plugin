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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.obolibrary.robot.MergeOperation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command to check the alignment of an ontology against another, upper-level
 * ontology. The ontology is said to be “aligned” if all its classes are
 * subclasses of one of the upper ontology’s classes.
 * <p>
 * The command can also check “self-alignment” (option
 * <code>--use-self true</code>): the ontology is “self-aligned” if all its
 * classes are subclasses of one of the self-declared “preferred roots”
 * (declared within the ontology itself with the <code>IAO:0000700</code>
 * annotation).
 * <p>
 * Lastly, the command can check alignment against arbitrary root terms which
 * can be specified with the <code>--term</code> or <code>--term-file</code>
 * options.
 */
public class CheckAlignmentCommand extends BasePlugin {

    private static final Logger logger = LoggerFactory.getLogger(CheckAlignmentCommand.class);

    private Set<String> basePrefixes = new HashSet<>();
    private OWLDataFactory factory;

    public CheckAlignmentCommand() {
        super("check-align", "validate alignment with an upper ontology or with explicit roots",
                "robot validate [--upper-ontology[-iri] ONT] [--report-output FILE]");

        options.addOption("u", "upper-ontology", true, "load the upper ontology from the specified file");
        options.addOption("U", "upper-ontology-iri", true, "load the upper ontology from the specified IRI");
        options.addOption("C", "use-cob", true, "use COB as the upper ontology");

        options.addOption("t", "term", true, "check alignment against specified term");
        options.addOption("T", "term-file", true, "check alignment against specified list of terms");

        options.addOption("S", "use-self", true, "check self-alignment against declared roots");

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
        factory = state.getOntology().getOWLOntologyManager().getOWLDataFactory();

        // The classes we need to check alignment against
        Set<OWLClass> upperClasses = new HashSet<>();
        for ( IRI explicitRoot : CommandLineHelper.getTerms(ioHelper, line, true) ) {
            upperClasses.add(factory.getOWLClass(explicitRoot));
        }
        if ( CommandLineHelper.getBooleanValue(line, "use-self", false) ) {
            upperClasses.addAll(getPreferredRoots(state.getOntology()));
        }

        OWLOntology ontology = getUpperOntology(line);
        if ( ontology != null ) {
            // All classes from the upper ontology are treated as valid roots
            upperClasses.addAll(ontology.getClassesInSignature(Imports.INCLUDED));
            upperClasses.remove(factory.getOWLThing());

            // We merge the current ontology into the upper ontology rather than the other
            // way around, so that the current ontology remains unchanged and can be used
            // for further operations downstream in the ROBOT pipeline.
            MergeOperation.mergeInto(state.getOntology(), ontology, true, true);
        } else {
            // No upper ontology, validate against explicit and declared roots only
            ontology = state.getOntology();
        }

        if ( upperClasses.isEmpty() ) {
            logger.error("No roots to validate against");
            return;
        }
        if ( logger.isDebugEnabled() ) {
            for ( OWLClass klass : upperClasses ) {
                logger.debug("Using root class {}", klass.getIRI().toQuotedString());
            }
        }

        OWLReasoner reasoner = CommandLineHelper.getReasonerFactory(line).createReasoner(ontology);
        Set<OWLClass> unalignedClasses = getUnalignedClasses(ontology, reasoner, upperClasses, ignoreDangling);

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

    /*
     * Optionally loads an upper ontology from command line options.
     */
    private OWLOntology getUpperOntology(CommandLine line) throws IOException {
        OWLOntology ontology = null;
        if ( line.hasOption("upper-ontology") ) {
            ontology = ioHelper.loadOntology(line.getOptionValue("upper-ontology"), true);
        } else if ( line.hasOption("upper-ontology-iri") ) {
            ontology = ioHelper.loadOntology(getIRI(line.getOptionValue("upper-ontology-iri"), "upper-ontology-iri"));
        } else if ( CommandLineHelper.getBooleanValue(line, "use-cob", false) ) {
            ontology = ioHelper.loadOntology(Constants.COB_IRI);
        }

        return ontology;
    }

    /*
     * Extracts self-declared roots from the ontology.
     */
    private Set<OWLClass> getPreferredRoots(OWLOntology ontology) {
        Set<OWLClass> roots = new HashSet<>();
        for ( OWLAnnotation annot : ontology.getAnnotations() ) {
            if ( annot.getProperty().getIRI().equals(Constants.PREFERRED_ROOT_PROPERTY) ) {
                OWLAnnotationValue value = annot.getValue();
                if ( value.isIRI() ) {
                    roots.add(factory.getOWLClass(value.asIRI().get()));
                } else {
                    logger.warn("Ignoring non-IRI IAO:0000700 value: {}", value);
                }
            }
        }

        return roots;
    }

    /*
     * Gets all top-level unaligned classes in the ontology.
     */
    private Set<OWLClass> getUnalignedClasses(OWLOntology ontology, OWLReasoner reasoner, Set<OWLClass> upperClasses,
            boolean ignoreDangling) {
        Set<OWLClass> unalignedClasses = new HashSet<>();
        for ( OWLClass klass : ontology.getClassesInSignature(Imports.INCLUDED) ) {
            if ( !klass.isTopEntity() && !upperClasses.contains(klass) && isInBase(klass.getIRI().toString()) ) {
                if ( ignoreDangling && Util.isDangling(ontology, klass) ) {
                    continue;
                }
                if ( Util.isObsolete(ontology, klass) ) {
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
                    if ( ancestors.size() == 1 ) {
                        // This is already a top-level class
                        logger.debug("Unaligned class: {}", klass.getIRI().toQuotedString());
                        unalignedClasses.add(klass);
                    } else {
                        // Find the top-level ancestor(s)
                        for ( OWLClass ancestor : ancestors ) {
                            if ( reasoner.getSuperClasses(ancestor, false).isTopSingleton() ) {
                                logger.debug("Unaligned class: {} (from {})", ancestor.getIRI().toQuotedString(),
                                        klass.getIRI().toQuotedString());
                                unalignedClasses.add(ancestor);
                            }
                        }
                    }
                }
            }
        }

        return unalignedClasses;
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

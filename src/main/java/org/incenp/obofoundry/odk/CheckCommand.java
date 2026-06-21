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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckCommand extends BasePlugin {

    private static final Logger logger = LoggerFactory.getLogger(CheckCommand.class);

    private Set<String> basePrefixes = new HashSet<>();
    private Map<OWLEntity, Boolean> cache = new HashMap<>();

    public CheckCommand() {
        super("check", "perform some checks on an ontology", "robot check --checks [CHECK,...]");

        options.addOption("c", "checks", true, "list of checks to perform");
        options.addOption(null, "base-iri", true, "only check entities in the indicated namespace(s)");
        options.addOption("x", "fail", true, "if true, fail if any of the checks fails");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        for ( String prefix : CommandLineHelper.getOptionalValues(line, "base-iri") ) {
            basePrefixes.add(getIRI(prefix, "base-iri").toString());
        }
        cache.clear();

        int failed = 0;
        for ( String check : line.getOptionValues("checks") ) {
            switch ( check ) {
            case "deprecated-references":
                if ( !checkDeprecatedReferences(state.getOntology()) ) {
                    logger.error("The ontology contains references to deprecated entities");
                    failed += 1;
                }
                break;
            }
        }

        if ( failed > 0 && CommandLineHelper.getBooleanValue(line, "fail", false) ) {
            System.exit(1);
        }
    }

    private boolean checkDeprecatedReferences(OWLOntology ontology) {
        boolean pass = true;
        for ( OWLEntity entity : ontology.getSignature(Imports.INCLUDED) ) {
            if ( !isInBase(entity.getIRI().toString()) || isDeprecated(ontology, entity) ) {
                continue;
            }

            Set<OWLAxiom> axioms = new HashSet<>();
            if ( entity instanceof OWLClass ) {
                axioms.addAll(ontology.getAxioms((OWLClass) entity, Imports.INCLUDED));
            } else if ( entity instanceof OWLObjectProperty ) {
                axioms.addAll(ontology.getAxioms((OWLObjectProperty) entity, Imports.INCLUDED));
            } else if ( entity instanceof OWLDataProperty ) {
                axioms.addAll(ontology.getAxioms((OWLDataProperty) entity, Imports.INCLUDED));
            } else if ( entity instanceof OWLNamedIndividual ) {
                axioms.addAll(ontology.getAxioms((OWLNamedIndividual) entity, Imports.INCLUDED));
            }

            for ( OWLAxiom axiom : axioms ) {
                for ( OWLEntity referenced : axiom.getSignature() ) {
                    if ( isDeprecated(ontology, referenced) ) {
                        pass = false;
                        logger.warn("{} references deprecated entity {}", entity.getIRI(), referenced.getIRI());
                    }
                }
            }
        }

        for ( OWLOntology ont : ontology.getImportsClosure() ) {
            for ( OWLClassAxiom gca : ont.getGeneralClassAxioms() ) {
                boolean hasEntitiesInBase = false;
                List<String> deprecatedEntities = new ArrayList<>();
                for ( OWLEntity referenced : gca.getSignature() ) {
                    if ( isInBase(referenced.getIRI().toString()) ) {
                        hasEntitiesInBase = true;
                    }
                    if ( isDeprecated(ontology, referenced) ) {
                        deprecatedEntities.add(referenced.getIRI().toString());
                    }
                }
                if ( hasEntitiesInBase && !deprecatedEntities.isEmpty() ) {
                    pass = false;
                    logger.warn("A general class axiom references deprecated entities: {}",
                            String.join(", ", deprecatedEntities));
                }
            }
        }

        return pass;
    }

    private boolean isDeprecated(OWLOntology ontology, OWLEntity entity) {
        Boolean deprecated = cache.get(entity);
        if ( deprecated != null ) {
            return deprecated;
        }

        deprecated = Util.isObsolete(ontology, entity);
        cache.put(entity, deprecated);
        return deprecated;
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

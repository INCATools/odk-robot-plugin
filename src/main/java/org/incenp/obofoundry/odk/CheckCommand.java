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

/**
 * A command to perform some quality control checks on an ontology.
 * <p>
 * Checks to perform are indicated with the <code>--check NAME,...</code>
 * option, where <code>NAME</code> is a check to enable.
 * <p>
 * Currently, the only available check is <code>deprecated-references</code>,
 * which checks whether the ontology contains references to any deprecated
 * entity.
 */
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

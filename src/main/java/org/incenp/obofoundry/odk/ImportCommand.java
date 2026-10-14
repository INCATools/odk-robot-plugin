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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveImport;

/**
 * A command to edit import declarations within an ontology.
 */
public class ImportCommand extends BasePlugin {

    public ImportCommand() {
        super("import", "add/remove import declarations", "robot import [--add IMPORT|--remove IMPORT]");

        options.addOption(null, "add", true, "inject an import declaration");
        options.addOption(null, "remove", true, "remove an import declaration");
        options.addOption(null, "exclusive", true,
                "if true, replace any existing declarations by the ones set by --add");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        OWLOntology ontology = state.getOntology();
        OWLDataFactory fac = ontology.getOWLOntologyManager().getOWLDataFactory();
        ArrayList<OWLOntologyChange> changes = new ArrayList<>();

        for ( String imp : CommandLineHelper.getOptionalValues(line, "add") ) {
            changes.add(new AddImport(ontology, fac.getOWLImportsDeclaration(IRI.create(imp))));
        }
        for (String imp : CommandLineHelper.getOptionalValues(line, "remove")) {
            changes.add(new RemoveImport(ontology, fac.getOWLImportsDeclaration(IRI.create(imp))));
        }

        if ( CommandLineHelper.getBooleanValue(line, "exclusive", false) ) {
            Set<String> added = new HashSet<>(CommandLineHelper.getOptionalValues(line, "add"));
            for ( OWLImportsDeclaration decl : ontology.getImportsDeclarations() ) {
                if ( !added.contains(decl.getIRI().toString()) ) {
                    changes.add(new RemoveImport(ontology, decl));
                }
            }
        }

        ontology.getOWLOntologyManager().applyChanges(changes);
    }
}

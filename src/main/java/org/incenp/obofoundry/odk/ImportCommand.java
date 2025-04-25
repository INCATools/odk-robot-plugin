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

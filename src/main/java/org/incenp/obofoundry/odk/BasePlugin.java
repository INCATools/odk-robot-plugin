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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.obolibrary.robot.Command;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.obolibrary.robot.IOHelper;
import org.semanticweb.owlapi.model.IRI;

/**
 * Helper base class for ROBOT commands.
 * <p>
 * This class is intended to serve as a base class for ROBOT commands, to avoid
 * duplicating boilerplate across several commands. Subclasses should call the
 * constructor with the desired name, description, and help message, add any
 * option they need, and implement the
 * {@link #performOperation(CommandState, CommandLine)} method.
 */
public abstract class BasePlugin implements Command {

    private String name;
    private String description;
    private String usage;
    protected Options options;
    protected IOHelper ioHelper;

    /**
     * Creates a new command.
     * 
     * @param name  The command name, as it should be invoked on the command line.
     * @param desc  The description of the command that ROBOT will display.
     * @param usage The help message for the command.
     */
    protected BasePlugin(String name, String desc, String usage) {
        this.name = name;
        this.description = desc;
        this.usage = usage;
        options = CommandLineHelper.getCommonOptions();
        options.addOption("i", "input", true, "load ontology from file");
        options.addOption("I", "input-iri", true, "load ontology from IRI");
        options.addOption("o", "output", true, "save ontology to file");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUsage() {
        return usage;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void main(String[] args) {
        try {
            execute(null, args);
        } catch ( Exception e ) {
            CommandLineHelper.handleException(e);
        }
    }

    @Override
    public CommandState execute(CommandState state, String[] args) throws Exception {
        CommandLine line = CommandLineHelper.getCommandLine(usage, options, args);
        if ( line == null ) {
            return null;
        }

        ioHelper = CommandLineHelper.getIOHelper(line);
        state = CommandLineHelper.updateInputOntology(ioHelper, state, line);

        performOperation(state, line);

        CommandLineHelper.maybeSaveOutput(line, state.getOntology());

        return state;
    }

    /**
     * Performs whatever operation the command is supposed to do.
     * 
     * @param state The internal state of ROBOT.
     * @param line  The command line used to invoke the command.
     * @throws Exception If any error occurs when attempting to execute the
     *                   operation.
     */
    public abstract void performOperation(CommandState state, CommandLine line) throws Exception;

    /**
     * Creates an IRI from a user-specified source. This delegates the task of
     * expanding CURIEs to ROBOT, which may use whatever information it has (such as
     * prefix mappings specified on the command line with the {@code --prefix}
     * option).
     * 
     * @param term  The term to transform into an IRI.
     * @param field The source where the term comes from. Used in ROBOT's error
     *              message, if the term cannot be transformed into an IRI.
     * @return The resulting IRI.
     * @throws IllegalArgumentException If the term cannot be transformed into an
     *                                  IRI.
     */
    protected IRI getIRI(String term, String field) {
        return CommandLineHelper.maybeCreateIRI(ioHelper, term, field);
    }

    /**
     * Reads a file and gets its contents as a set, one entry per line, excluding
     * any blank line and lines starting with a '#' character.
     * 
     * @param filename The name of the file to read.
     * @return The file's lines as a set of unique strings.
     * @throws IOException If any I/O error occurs when reading the file.
     */
    protected Set<String> readFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        Set<String> lines = new HashSet<>();
        String line = null;
        while ( (line = reader.readLine()) != null ) {
            line = line.trim();
            if ( !line.isEmpty() && line.charAt(0) != '#' ) {
                lines.add(line);
            }
        }
        reader.close();

        return lines;
    }

    /**
     * Reads a file and gets its contents as a set of IRIs, assuming one IRI per
     * line, excluding any blank line and lines starting with a '#' character.
     * CURIEs that cannot be converted to a full-length IRI are silently ignored.
     * 
     * @param filename The name of the file to read.
     * @return The set of IRIs contained in the file.
     * @throws IOException If any I/O error occurs when reading the file.
     */
    protected Set<IRI> readFileAsIRIs(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        Set<IRI> terms = new HashSet<>();
        String line = null;
        while ( (line = reader.readLine()) != null ) {
            line = line.trim();
            if ( !line.isEmpty() && line.charAt(0) != '#' ) {
                IRI termIRI = ioHelper.createIRI(line);
                if ( termIRI != null ) {
                    terms.add(termIRI);
                }
            }
        }
        reader.close();

        return terms;
    }
}

ODK ROBOT plugin
================

This is a [ROBOT](http://robot.obolibrary.org/) plugin intended to be
used within the [Ontology Development
Kit](https://github.com/INCATools/ontology-development-kit) (ODK). It
provides additional ROBOT commands to perform tasks not covered by the
ROBOT built-in command set.

Provided commands
-----------------

### validate
This command is intended to check the alignment of the ontology against
another, “upper-level” ontology. The ontology is considered “aligned” if
all its classes are subclasses of at least one class of the upper
ontology.

The upper ontology to check against is specified using either the
`--upper-ontology` option (to load it from a local file) or the
`--upper-ontology-iri` option (to load it from a resolvable IRI). If
neither option is used, the command defaults to check against the [Core
Ontology for Biology and Biomedicine](https://github.com/OBOFoundry/COB)
(COB).

By default, the command will _fail_ (interrupting any pipeline it is a
part of in the process) if the ontology is not aligned. Use the
`--fail false` option to allow the pipeline to continue without
erroring out.

The command will produce a report listing the top-level unaligned
classes (if any) if the `--report-output <FILE>` option is used.

To only check the alignment of classes in a given namespace, use the
`--base-iri <namespace>` option – that option may be used repeatedly to
check classes in different namespaces.

If the `--ignore-dangling true` option is used, classes that are
considered “dangling” will be ignored. You may want to use that option
if you are not restricting the alignment check to your ontology’s
namespace, to avoid failing the check for any single dangling class
that is merely referenced from your ontology. On the other hand, not
using that option could be a way to check that your ontology does _not_
reference dangling classes (which could indicate that you are missing
some imports).

### normalize
This command is intended to perform various “normalisation” operations.

The main normalisation operation is the injection of
_SubAnnotationPropertyOf_ axioms for annotation properties that
represent subsets (`--subset-decls true`) or synonym types
(`--synonym-decls true`).

For subsets, if the ontology contains any annotation using the
`oboInOwl:inSubset` property (which marks a class as belonging to a
subset), and the annotation value is an IRI, this will inject an axiom
declaring that the IRI represents a subproperty of
`oboInOwl:SubsetProperty`.

For synonyms, if the ontology contains any annotation using the
`oboInOwl:hasSynonymType` property (used on synonyn annotations to
indicate the nature of the synonym, e.g. abbreviations, etc.), and the
annotation value is an IRI, this will inject an axiom declaring that the
IRI represents a subproperty of `oboInOwl:SynonymTypeProperty`.

In both cases, the normalisation will only consider IRIs that fall under
the following namespaces:

* `http://purl.obolibrary.org/obo/`;
* `http://www.ebi.ac.uk/efo/`;
* `http://w3id.org/biolink/`.

Additional namespaces can be specified using the `--base-iri` option.

Another normalisation operation is the merging of duplicate axioms that
differ only by their annotations. This is done with the
`--merge-axioms true` option. Of note, the existence of that option is
a (hopefully) temporary workaround until the corresponding feature in
ROBOT (`robot repair --merge-axiom-annotations true`) is fixed, as it
currently does not work as expected.

Lastly, the command can also inject a `dc:source` annotation on the
ontology, set from the version IRI (`--add-source true`). This is used
in the ODK to annotate import modules with the version IRI of the
ontology they are derived from. (It is hoped that this could soon be
replaced by a built-in feature in ROBOT’s `annotate` command.)

### subset
The `subset` command creates an ontology subset. It is intended to
replace several equivalent OWLTools commands with a uniformed behaviour.

#### Subset definition
The command offers several ways of defining which classes should be
included in the subset:

* With a DL query, e.g `--query "'part of' some 'nervous system'"`

The subset will include all equivalent classes and subclasses matching
the query (to include superclasses as well, add the `--ancestors true`
option). The query can use either quoted labels (as in the example
above) or short-form identifiers (e.g. `--query UBERON:0001016`).
When using quoted labels, if the query consists of a single class, be
mindful that you will most likely need to quote it _twice_, as in
`--query "'nervous system'"` – the outer quotes will be stripped by
your command interpreter.

Be also mindful that not all reasoners allow querying using a class
expression – ELK, which the `subset` command uses by default, does not,
so you might want to use WHELK instead (`--reasoner WHELK`).

* With a subset name or IRI, e.g. `--subset PFX:MY_SUBSET`

This will select all classes that are marked with a `oboInOwl:inSubset`
annotation whose value is the `PFX:MY_SUBSET` IRI. If the argument is
not an IRI (e.g. `--subset MY_SUBSET`), this will select all classes
with a `oboInOwl:inSubset` annotation whose value ends with `#MY_SUBSET`
regardless of the namespace – this is for compatibility with OWLTools.

* With an explicit list of terms (`--term TERM` or `--term-file FILE`)

Any class whose ID is explicitly specified on the command line with
`--term CLASS:ID` or is listed in the file pointed to by the argument
to `--term-file` (which is expected to contain a list of IDs, with one
ID per line, excluding blank lines and lines starting with `#`) will be
included in the subset.

The `--query`, `--subset`, `--term`, and `--term-file` option can be
mixed freely and used repeatedly. Their effects are cumulative. For
example:

```sh
odk:subset --reasoner WHELK \
           --query "'nervous system'" \
           --query "'part of' some 'nervous system'" \
           --term UBERON:0000955
```

will create a subset from (1) 'nervous system' and all its descendants
and equivalents, (2) all classes that are 'part of' the 'nervous
system', and (3) the UBERON:0000955 class.

#### Expanding the subset
By default, the subset generated by the `subset` command contains _only_
the classes defined using any of the methods shown in the previous
section, plus all the object and annotation properties used by those
classes.

Use the `--fill-gaps true` option to expand the subset so that it
contains all the classes that are referenced from within the initial
subset.

Several options allow to control how the subset is expanded.

* `--follow-property PROPERTY`

By default, the expanded subset will include all classes referenced by
_any_ class expression used within the initial subset. If the
`--follow-property PROPERTY` option is used (where `PROPERTY` is the
IRI of an object property), only class expressions that use the
indicated object property will be considered. The option may be used
several times to follow several object properties.

* `--follow-in NAMESPACE`

When this option is used, only classes that are in the indicated
namespace will be included in the expanded subset (the option may be
used several times to include classes from several namespaces). Axioms
that refer to a class outside of the followed namespace(s) will be
excluded from the subset.

* `--not-follow-in NAMESPACE`

This is the opposite of the previous option. It prevents the inclusion
of any classes that is in the indicated namespace. It may also be used
repeatedly to exclude classes from several namespaces. Axioms that refer
to a class within the not-followed namespace(s) will be excluded from
the subset.

The `--follow-in` and `--not-follow-in` options are mutually
exclusive. If both are used in the same `subset` command, the
`--follow-in` options take precedence and any `--not-follow-in` option
will be ignored.

* `--no-dangling false`

By default, “dangling” classes are not considered for inclusion when
expanding the subset, even if they are referenced from within the
subset. Use that option to include them anyway.

Note that none of the 4 options above affect the _initial subset_ (the
subset defined by any of the `--query`, `--subset`, `--term`, or
`--term-file` options). They only affect how the subset is expanded. For
example, if the initial subset contains a class in the `GO:` namespace,
that class will be present in the final subset even if the
`--not-follow-in http://purl.obolibrary.org/obo/GO_` option is used. To
force the exclusion of _any_ GO class, either make sure that the initial
subset does not list any such class, or forcibly remove all GO classes
from the ontology (e.g. with `robot remove` or `robot filter`) before
creating the subset.

#### Writing the subset
By default, once the subset is created, it becomes the main ontology
that is being manipulated by the ROBOT pipeline (replacing the initial
ontology). This means that:

* it can be saved to file using the traditional `--output` option;
* it will be passed down to any further ROBOT command.

If you use the `--write-to FILE` option, the subset will be saved into
the indicated file, and will _not_ be passed down to the rest of the
ROBOT pipeline (the unmodified, initial ontology will be passed down
instead). This allows creating several subsets from the same ontology:

```sh
robot merge -i my-ontology.owl \
      odk:subset --subset MY_SUBSET --write-to my-subset.owl \
      odk:subset --subset ANOTHER_SUBSET --write-to another-subset.owl
```


Building
--------
Build with Maven by running:

```sh
mvn clean package
```

This will produce two Jar files in the `target` directory.

The `odk.jar` file is the plugin itself. Place this file in your ROBOT
plugins directory (by default `~/.robot/plugins`), then call the
commands by prefixing them with the basename of the Jar file in the
plugins directory.

For example, if you placed the plugin at `~/.robot/plugins/odk.jar`,
you may call the `subset` command as follows:

```sh
robot odk:subset ...
```

The `odk-robot-standalone-X.Y.Z.jar` file is a standalone version of
ROBOT that includes the commands provided by this plugin as is they were
built-in commands. It is mostly intended for testing and debugging, as
it allows using the commands from the plugin without having to actually
install the plugin in a ROBOT plugins directory.

Using with the ODK
------------------
The plugin is (or will be) provided with the ODK Docker image. To use it
as part of a ODK workflow, all that is needed is to make the rule in
which the plugin is to be used depend on the ODK built-in rule
`all_robot_plugins`. This will make the plugin available in the
repository’s `src/ontology/tmp/plugins` directory, which is already set,
in ODK workflows, as the ROBOT plugins directory.

For example:

```make
target.owl: source1.owl source2.owl | all_robot_plugins
        $(ROBOT) merge -i source1.owl -i source2.owl \
                 odk:subset --subset MY_SUBSET \
                            --output target.owl
```

The plugin can also be used outside of any ODK workflow, by manually
instructing ROBOT to look for plugins into the `/tools/robot-plugins/`
directory (e.g. by setting the `ROBOT_PLUGINS_DIRECTORY` environment
variable to that directory).

Copying
-------
The ODK ROBOT plugin is distributed under the same terms as the Ontology
Development Kit itself (3-clause BSD license). See the
[COPYING file](COPYING) in the source distribution.

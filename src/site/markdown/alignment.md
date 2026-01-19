Validating against a upper ontology
===================================

The `odk:check-align` command is intended to check the alignment of the
input ontology against another, “upper-level” ontology. The ontology is
considered “aligned” if all its classes are subclasses of at least one
class of the upper ontology.

Specifying the upper ontology
-----------------------------
The upper ontology to check against is specified using either the
`--upper-ontology <FILE>` option (to load it from a local file) or the
`--upper-ontology-iri <IRI>` option (to load it from a resolvable IRI).

Alternatively, the `--use-cob true` option can be used to automatically
load the latest version of the
[Core Ontology for Biology and Biomedicine](https://github.com/OBOFoundry/COB),
from its standard PURL of `http://purl.obolibrary.org/obo/cob.owl`.

Alignment with arbitrary root terms
-----------------------------------
The `odk:check-align` command can also be used to check alignment
against an arbitrary set of root terms, independently of any upper-level
ontology.

Root terms to check alignment against can be specified using the
`--term` option (one term per option) or the `--term-file` option (which
expects a path to a file containing one term per line, ignoring blank
lines and lines starting with `#`).

Use the `--use-self true` to perform a “self-alignment” check: this will
use any “preferred root term” self-declared within the ontology itself
by means of the `IAO:0000700` annotation.

Those options, as well as the options from the previous section, have
cumulative effects. For example, the following command:

```
robot odk:check-align --input uberon.owl \
                      --use-self true \
                      --term UBERON:0000000
```

will check the alignment of UBERON classes against both its
self-declared roots and the additional `UBERON:0000000` root. That is,
the ontology will be considered “aligned” if all its classes are
subclasses of any of the self-declared roots or of `UBERON:0000000`.

Similarly, with the following command:

```
robot odk:check-align --input uberon.owl \
                      --use-cob true \
                      --use-self true
```

the ontology will be considered “aligned” if all its subclasses are
subclasses of any of the COB roots or of any of the self-declared roots.

To check that all classes are subclasses of any of the upper ontology
roots _and_ additionally that they are also subclasses of any of the
self-declared roots, perform the two checks separately:

```
robot odk:check-align --input uberon.owl \
                      --use-cob true \
      odk:check-align --use-self true
```

Notes about reasoning
---------------------
The alignment check requires the use of a reasoner. As with all ROBOT
commands that needs a reasoner, you can specify the reasoner to use with
the `-r`, `--reasoner` option.

Importantly, limitations of the reasoner used may cause some classes to
be falsely flagged as non-aligned, if they are logically defined using
some class expressions that are not fully supported by the reasoner. To
avoid that, if the ontology is known to use class expressions that the
reasoner might not support, it is advised to relax the ontology prior to
checking its alignment:

```
robot relax --input uberon.owl \
      odk:check-align --use-cob true
```

Validation results
------------------
By default, the command will _fail_ (forcibly interrupting any pipeline
it is a part of in the process) if the ontology is not aligned.

Use the `--fail false` option to allow the pipeline to continue without
erroring out.

If the pipeline continues (either because the ontology is aligned, or
because `--fail false` was used), the next command in the pipeline
receives the original, unmodified ontology.

To produce a report from the validation results, use the
`--report-output <FILE>` option. What will be included in this report is
controlled by the `--detail <MODE>` option:

* `--detail ROOT` will report all the top-level classes (if any) of the
  input ontology that are not aligned against the upper ontology; this is
  the default behaviour;
* `--detail BASE-ROOT` will report the highest “in-base” classes that
  are not aligned and all their ancestors up to the top-level classes;
  this is only meaningful if `--base-iri` is also used, otherwise this
  is the same as `--detail ROOT`;
* `--detail ALL` will report all unaligned classes.

Restricting the scope of the validation
---------------------------------------
By default, the command checks the alignment of every single class found
in the input ontology.

To only check the alignment of classes in a given namespace, use the
`--base-iri <NAMESPACE>` option – that option may be used repeatedly to
check classes in more than one namespace.

For example, to check the alignment of all classes in the OBO UBERON
namespace against COB:

```
robot odk:check-align --input uberon.owl \
                      --base-iri http://purl.obolibrary.org/obo/UBERON_
```

Furthermore, if the `--ignore-dangling true` option is used, dangling
classes are ignored. In the context of this command, a class is
considered “dangling” if the ontology contains _no_ defining axioms
(excluding disjointness axioms) and _no_ annotation assertion axioms for
the class. You may want to use that option if you are not restricting
the alignment check to your ontology’s namespace, to avoid failing the
check for any single dangling class that is merely referenced from your
ontology. On the other hand, including dangling classes in the check
(which is the case by default) could be a way to check that your
ontology does _not_ reference dangling classes (which could indicate
that you are missing some imports).

Validating against a upper ontology
===================================

The `odk:validate` command is intended to check the alignment of the
input ontology against another, “upper-level” ontology. The ontology is
considered “aligned” if all its classes are subclasses of at least one
class of the upper ontology.

Specifying the upper ontology
-----------------------------
The upper ontology to check against is specified using either the
`--upper-ontology <FILE>` option (to load it from a local file) or the
`--upper-ontology-iri <IRI>` option (to load it from a resolvable IRI).

If neither option is used, the command defaults to check against the
latest version of the [Core Ontology for Biology and Biomedicine](https://github.com/OBOFoundry/COB) (COB), which is loaded from its standard PURL of
`http://purl.obolibrary.org/obo/cob.owl`.

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
`--report-output <FILE>` option. The file will contain a list of all the
top-level classes (if any) of the input ontology that are not aligned
against the upper ontology.

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
robot odk:validate --input uberon.owl \
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

Normalizing an ontology
=======================

The `odk:normalize` command is intended to perform various
“normalisation” operations.

By default, the command does nothing. Each of the normalisation
operations described below must be explicitly enabled with its
corresponding option.

The special flag `--all` enables all the available normalisation
operations. When it is used, a particular operation can still be
disabled by explicitly using the corresponding option with a `false`
argument. For example, to perform all possible normalisations
operations _except_ the merging of duplicated axioms:

```
robot odk:normalize --all --merge-axioms false
```

Injecting declarations for subset properties
--------------------------------------------
This is requested with the `--subset-decls true` option.

When enabled, if the ontology contains any annotation using the
`oboInOwl:inSubset` property (which is used, across OBO ontologies, to
mark a class as belonging to a given subset), and the annotation value
is an IRI, this command will inject an axiom declaring that the IRI
represents a subproperty of `oboInOwl:SubsetProperty`.

The command will only consider IRIs that fall under the following
namespaces:

* `http://purl.obolibrary.org/obo/`;
* `http://www.ebi.ac.uk/efo/`;
* `http://w3id.org/biolink/`.

Additional namespaces can be specified using the `--base-iri <NAMESPACE>`
option.

Injecting declarations for synonym type properties
--------------------------------------------------
This is requested using the `--synonym-decls true` option.

When enabled, this performs a task similar to `--subset-decls true`,
but for properties that represent synonym types. That is, if the
ontology contains any annotation using the `oboInOwl:hasSynonymType`
property (used on synonym annotations to indicate the nature of the
synonym, e.g. abbreviations, etc) and the annotation is an IRI, the
command will inject an axiom declaring that the IRI represents a
subproperty of `oboInOwl:SynonymTypeProperty`.

The command will consider IRIs that fall under the namespaces listed in
the previous section. Additional namespaces can be specified using the
`--base-iri <NAMESPACE>` option.

(The `--base-iri` option thus affects both the `--subset-decls` and the
`--synonym-decls` features. If you want to use different namespaces for
each operation, you must invoke the `odk:normalize` command twice, one
time to inject subset declarations, and a second time to inject synonym
type declarations.)

Merging duplicated axioms
-------------------------
This is requested using the `--merge-axioms true` option.

When enabled, this will merge duplicated axioms that differ only by
their annotations. For example, if the ontology contains the following
axioms:

```
SubClassOf(UBERON:1234 UBERON:5678)
SubClassOf(Annotation(oboInOwl:hasDbXref "PMID:9999") UBERON:1234 UBERON:5678)
SubClassOf(Annotation(rdfs:comment "To be checked") UBERON:1234 UBERON:5678)
```

they will be replaced by a single axiom as follows:

```
SubClassOf(Annotation(oboInOwl:hasDbXref "PMID:9999")
           Annotation(rdfs:comment "To be checked")
           UBERON:1234 UBERON:5678)
```

That option is a (hopefully) temporary workaround until the
corresponding feature in ROBOT
(`robot repair --merge-axiom-annotations true`) is fixed, as it does
not currently (as of ROBOT 1.9.7) work as expected.

Injecting a dc:source annotation
--------------------------------
This is requested using the `add-source` option.

When enabled, this will inject a `dc:source` annotation ontology, set
from the version IRI of the input ontology. This is used in the ODK to
annotate import modules with the version IRI of the upstream ontology
they are derived from.

(It is hoped that this could soon be replaced by a built-in feature in
ROBOT’s `annotate` command.)

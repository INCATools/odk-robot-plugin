Obsoleting and replacing entities
=================================

The `odk:obsolete-replace` command is intended to obsolete an entity and
replace all its usages with a replacement entity in a single operation.

This is useful when merging terms or when an entity needs to be retired
but its usages should be redirected to another term.

Basic usage
-----------
The command requires two options:

* `--obsolete <TERM>`: The entity to obsolete (CURIE or IRI)
* `--replacement <TERM>`: The replacement entity (CURIE or IRI)

Example:

```
robot odk:obsolete-replace --input ontology.owl \
                           --obsolete EXAMPLE:0000001 \
                           --replacement EXAMPLE:0000002 \
                           --output output.owl
```

What the command does
---------------------
When executed, the command performs the following operations:

1. **Replaces all usages** of the obsolete entity with the replacement
   entity throughout the ontology (in all axioms)
2. **Removes annotations** from the obsolete entity: label, comment, and
   definition (IAO:0000115)
3. **Marks the obsolete entity as deprecated** by adding an
   `owl:deprecated true` annotation
4. **Adds a new label** to the obsolete entity, prefixed with "obsolete"
   (e.g., "obsolete old term name")
5. **Adds the original label as an exact synonym** on the replacement
   entity, with a database cross-reference annotation pointing to the
   obsolete entity
6. **Adds a "term replaced by" annotation** (IAO:0100001) on the
   obsolete entity pointing to the replacement entity

Example workflow
----------------
A typical workflow might look like:

```
robot merge --input ontology.owl \
      odk:obsolete-replace --obsolete GO:0000001 \
                           --replacement GO:0000002 \
      annotate --ontology-iri http://example.org/ontology.owl \
               --output output.owl
```

This merges the ontology, obsoletes `GO:0000001` replacing it with
`GO:0000002`, and then annotates and saves the result.

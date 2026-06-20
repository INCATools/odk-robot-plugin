Extracting referenced ORCID individuals
=======================================

The `odk:extract-orcids` command is intended to facilitate support for
[ORCIDIO](https://github.com/cthoyt/orcidio) within the Ontology
Development Kit.

Given an input ontology, it finds all ORCID identifiers referenced in
the ontology, then extracts from ORCIDIO the corresponding named
individuals. It saves those individuals into an output ontology which
can then be imported into the original ontology.

Example
-------
Simplest possible use is as follows:

```sh
robot odk:extract-orcids -i myont.owl -o orcids.owl
```

The newly created `orcids.owl` file will contain the axioms defining the
individuals that represent all the ORCID identifiers referenced in
annotations within the `myont.owl` ontology.

Finding ORCID references
------------------------
By default, `odk:extract-orcids` looks for ORCID references in all
IRI-valued annotations in the input ontology. To look for ORCID
references only in annotation that use a specific annotation property,
use the `--property <IRI>` option.

For example, to extract ORCID references in
`http://purl.org/dc/terms/contributor` annotations only, use
`--property dc:contributor`.

The option may be repeated as needed to find ORCID references in
annotations using several annotation properties.

ORCIDIO source
--------------
By default, `odk:extract-orcids` fetches the ORCIDIO ontology from its
canonical online source at `https://w3id.org/orcidio/orcidio.owl`. Use
the `--orcidio-iri <IRI>` option to download the ORCIDIO ontology from
another IRI, or the `--orcidio-file <FILE>` option to read the ORCIDIO
ontology from the specified file.

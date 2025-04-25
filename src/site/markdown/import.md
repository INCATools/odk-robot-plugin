Managing import declarations
============================

The `odk:import` command allows to programmatically add or remove import
declarations from an ontology.

It is primarily intended for internal use by the ODK itself (which uses
it when updating an ODK repository).

Adding an import declaration
----------------------------

Use the `--add <IRI>` option to add an import declaration. Repeat the
option to add as many declarations as needed:

```
robot odk:import -i myont-edit.owl \
                 --add http://purl.obolibrary.org/obo/myont/imports/a_import.owl \
                 --add http://purl.obolibrary.org/obo/myont/imports/b_import.owl \
      convert -f ofn -o myont-edit.owl
```

The option will be ignored if the input file happens to already contain
a declaration for the same import.

Removing an import declaration
------------------------------

Conversely, use the `--remove <IRI>` option to remove an import
declaration. Again, the option may be used repeatedly in the same
command:

```
robot odk:import -i myont-edit.obo \
                 --remove http://purl.obolibrary.org/obo/myont/imports/a_import.owl \
                 --remove http://purl.obolibrary.org/obo/myont/imports/b_import.owl \
      convert --check false -o myont-edit.obo
```

Exclusive mode
--------------

With the `--exclusive true` option, the command will also _remove_ all
import declarations that do not correspond to an import IRI added with
any `--add` option.

Use that option to ensure that an ontology contains import declarations
for a fixed set of import IRIs _and only for those IRIs_.

For example, the following command:

```
robot odk:import -i myont-edit.owl \
                 --add http://purl.obolibrary.org/obo/myont/imports/a_import.owl \
                 --add http://purl.obolibrary.org/obo/myont/imports/b_import.owl \
                 --exclusive true \
      convert -f ofn -o myont-edit.owl
```

will (1) _add_ the declarations for the `a_import.owl` and
`b_import.owl` modules, and (2) _remove_ any other declaration.

Use that option without any `--add` option to forcefully remove _all_
import declarations from the ontology.

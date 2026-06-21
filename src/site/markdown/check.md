Quality control checks
======================

The `odk:check` command is intended to run various QC checks on the
input ontology.

The checks to perform are indicated with the `--check <NAME,...>`
option.

When a check fails, an error message is logged in ROBOT’s output.
Additionally, if the `--fail true` option has been specified, any
failure will cause the command to error out and terminate the ROBOT
pipeline the command was part of.

Available checks
----------------
The following checks are currently available for this command:

* `deprecated-references`: fails if the ontology contains any reference
  to an entity that is marked as `owl:deprecated`.

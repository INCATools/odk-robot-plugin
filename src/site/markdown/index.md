ODK ROBOT Plugin
================

This is a [ROBOT](http://robot.obolibrary.org/) plugin intended to be
used within the [Ontology Development
Kit](https://github.com/INCATools/ontology-development-kit) (ODK). It
provides additional ROBOT commands to perform tasks not covered by the
built-in command set of ROBOT.

Available commands
------------------
Currently, the ODK ROBOT plugin provides the following commands:

* [odk:subset](subset.html), to create ontology subsets;
* [odk:validate](validate.html), to check the alignment of an ontology
  against an upper-level ontology;
* [odk:normalize](normalize.html), to perform various normalisation
  operations on an ontology.
  
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
Development Kit itself (3-clause BSD license). See the [COPYING
file](https://github.com/INCATools/odk-robot-plugin/blob/main/COPYING)
in the source distribution.
  
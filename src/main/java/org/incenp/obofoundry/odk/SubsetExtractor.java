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

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class to extract subsets from an ontology.
 */
public class SubsetExtractor {

    private static final Logger logger = LoggerFactory.getLogger(SubsetExtractor.class);

    private OWLOntology source;
    private OWLDataFactory factory;
    private OWLReasoner reasoner;

    private boolean fillGaps = false;
    private boolean followAllProperties = true;
    private boolean noDangling = true;
    private Imports importMode = Imports.INCLUDED;
    private Set<OWLObjectProperty> followedProperties = new HashSet<>();
    private Set<String> includedPrefixes;
    private Set<String> excludedPrefixes;

    /**
     * Creates a new instance.
     * 
     * @param ontology The ontology to extract subsets from.
     * @param reasoner The reasoner to use when attempting to fill gaps.
     */
    public SubsetExtractor(OWLOntology ontology, OWLReasoner reasoner) {
        source = ontology;
        factory = source.getOWLOntologyManager().getOWLDataFactory();
        this.reasoner = reasoner;
    }

    /**
     * Enables or disables gap filling.
     * <p>
     * “Gap filling” (the terminology comes from <em>OWLTools</em>) is the process
     * of expanding the initial list of classes to extract to include all classes
     * referred to, directly or not, by any class, object or annotation property
     * within the subset. It is disabled by default.
     * 
     * @param enabled {@code true} to enable gap filling, {@code false} to disable
     *                it.
     */
    public void setFillGaps(boolean enabled) {
        fillGaps = enabled;
    }

    /**
     * Enables or disables excluding dangling classes.
     * <p>
     * In the context of subset extraction, we define a “dangling” class as a class
     * that has no defining axiom (excluding disjointness axioms) and no annotation
     * assertion axioms. When this option is enabled, dangling classes are excluded
     * from the gap filling process (meaning that we never extend the initial subset
     * to include a dangling class).
     * <p>
     * This option is only relevant when gap filling is enabled. A dangling class in
     * the initial subset is always included regardless of the status of this
     * option.
     * 
     * @param enabled {@code true} to exclude dangling classes, {@code false} to
     *                include them.
     */
    public void setExcludeDangling(boolean enabled) {
        noDangling = enabled;
    }

    /**
     * Includes or excludes axioms from imported ontologies. They are included by
     * default.
     * 
     * @param enabled {@code true} to enable the use of axioms from the imported, or
     *                {@code false} to only use axioms from the main ontology.
     */
    public void includeImports(boolean enabled) {
        importMode = enabled ? Imports.INCLUDED : Imports.EXCLUDED;
    }

    /**
     * Adds an object property to follow when filling gaps.
     * <p>
     * The default behaviour when filling gaps is to include any class referenced
     * from one of the classes in the initial subset, regardless of the relation
     * between the referrer and the referee. When this method is used, classes that
     * are referred as part of a class expression involving an object property will
     * only be included if the object property matches the property added here.
     * <p>
     * This method may be called several times to allow following several object
     * properties.
     * <p>
     * Passing a {@code null} argument disables the following of all relations other
     * than subsumption relations.
     * 
     * @param propertyIRI The IRI of an object property to follow when filling gaps.
     */
    public void followProperty(IRI propertyIRI) {
        if ( propertyIRI != null ) {
            followedProperties.add(factory.getOWLObjectProperty(propertyIRI));
        } else {
            followedProperties.clear();
        }
        followAllProperties = false;
    }

    /**
     * Only include classes within the given namespace when filling gaps.
     * <p>
     * When filling gaps, by default all referenced classes are included in the
     * extended subset. If this method is used, then only classes whose IRI starts
     * with the specified prefix will be included. This takes precedence over
     * “excluded prefixes” as added by the {@link #excludedPrefixes} method.
     * <p>
     * This method may be called several times to define several namespaces. A class
     * will be included if its IRI matches any of the included prefixes.
     * <p>
     * This has no effect on the initial subset. A class from the initial subset
     * will be included even if its IRI does not match any of the included prefixes.
     * 
     * @param prefix The IRI prefix to include.
     */
    public void includePrefix(String prefix) {
        if ( includedPrefixes == null ) {
            includedPrefixes = new HashSet<>();
        }
        includedPrefixes.add(prefix);
    }

    /**
     * Excludes classes within the given namespace when filling gaps.
     * <p>
     * If this method is used, then classes whose IRI starts with the specified
     * prefix will never be included when filling gaps. This method is incompatible
     * with the use of {@link #includedPrefixes}, which takes precedence if both
     * methods are used on the same instance.
     * <p>
     * This method may be called several times to define several namespaces. A class
     * will be included only if its IRI does not match any of the excluded prefixes.
     * <p>
     * This has no effect on the initial subset. A class from the initial subset
     * will never be excluded even if its IRI does match one of the excluded
     * prefixes.
     * 
     * @param prefix
     */
    public void excludePrefix(String prefix) {
        if ( excludedPrefixes == null ) {
            excludedPrefixes = new HashSet<>();
        }
        excludedPrefixes.add(prefix);
    }

    /**
     * Gets the list of classes that are marked as belonging to a given subset.
     * <p>
     * This method assumes that subsets are defined within the ontology using the
     * {@code oboInOwl:inSubset} annotation property.
     * 
     * @param subsetIRI The IRI of the subset whose classes should be retrieved.
     * @return The set of classes that make up the desired subset.
     */
    public Set<OWLClass> getSubset(IRI subsetIRI) {
        return getSubset(subsetIRI, factory.getOWLAnnotationProperty(Constants.IN_SUBSET));
    }

    /**
     * Gets the list of classes that are marked as belonging to a given subset.
     * 
     * @param subsetIRI      The IRI of the subset whose classes should be
     *                       retrieved.
     * @param subsetProperty The annotation property used to mark classes as
     *                       belonging to a subset.
     * @return The set of classes that make up the desired subset.
     */
    public Set<OWLClass> getSubset(IRI subsetIRI, OWLAnnotationProperty subsetProperty) {
        Set<OWLClass> subset = new HashSet<>();
        for ( OWLClass klass : source.getClassesInSignature(importMode) ) {
            for ( OWLAnnotationAssertionAxiom ax : source.getAnnotationAssertionAxioms(klass.getIRI()) ) {
                if ( ax.getProperty().equals(subsetProperty) ) {
                    if ( ax.getValue().isIRI() && ax.getValue().asIRI().get().equals(subsetIRI) ) {
                        subset.add(klass);
                    }
                }
            }
        }
        return subset;
    }

    /**
     * Gets the list of classes that are marked as belonging to a given subset,
     * where the subset is identified by a simple name.
     * <p>
     * This method is for compatibility with OWLTools’ {@code --subset} option. It
     * assumes (1) that subsets are defined using the {@code oboInOwl:inSubset}
     * annotation property, and (2) that subset IRIs are of the form
     * {@code URIBASE#SUBSET_NAME}.
     * 
     * @param subsetName The name of the subset whose classes should be retrieved.
     * @return The set of classes that make up the desired subset.
     */
    public Set<OWLClass> getSubset(String subsetName) {
        Set<OWLClass> subset = new HashSet<>();
        subsetName = "#" + subsetName;
        OWLAnnotationProperty prop = factory.getOWLAnnotationProperty(Constants.IN_SUBSET);
        for ( OWLClass klass : source.getClassesInSignature(importMode) ) {
            for ( OWLAnnotationAssertionAxiom ax : source.getAnnotationAssertionAxioms(klass.getIRI()) ) {
                if ( ax.getProperty().equals(prop) ) {
                    if ( ax.getValue().isIRI() && ax.getValue().asIRI().get().toString().endsWith(subsetName) ) {
                        subset.add(klass);
                    }
                }
            }
        }
        return subset;
    }

    /**
     * Creates a subset of the ontology.
     * 
     * @param subset The initial list of classes that make up the subset.
     * @return The created subset, as a new ontology.
     * @throws OWLOntologyCreationException If an error occurs when creating the
     *                                      ontology object.
     */
    public OWLOntology makeSubset(Set<OWLClass> subset) throws OWLOntologyCreationException {
        Set<OWLAxiom> axioms = new HashSet<>();
        Set<OWLClass> workSubset = new HashSet<>();
        Set<OWLClass> roundSubset = workSubset;
        workSubset.addAll(subset);
        int added = 0;
        int round = 0;

        do {
            int size = axioms.size();
            Set<OWLAxiom> classAxioms = new HashSet<>();
            Set<OWLAxiom> propertyAxioms = new HashSet<>();

            // Take care of the classes first
            if ( fillGaps ) {
                makeClassesClosure(roundSubset);
                workSubset.addAll(roundSubset);
            }
            includeClassAxioms(classAxioms, roundSubset, workSubset);

            // Then get all the properties used in classes' definitions
            Set<OWLObjectProperty> usedObjectProperties = new HashSet<>();
            Set<OWLAnnotationProperty> usedAnnotationProperties = new HashSet<>();
            for ( OWLAxiom ax : classAxioms ) {
                usedObjectProperties.addAll(ax.getObjectPropertiesInSignature());
                usedAnnotationProperties.addAll(ax.getAnnotationPropertiesInSignature());
            }
            if ( fillGaps ) {
                makeObjectPropertiesClosure(usedObjectProperties);
                makeAnnotationPropertiesClosure(usedAnnotationProperties);
            }
            includeObjectPropertyAxioms(propertyAxioms, usedObjectProperties);
            includeAnnotationPropertyAxioms(propertyAxioms, usedAnnotationProperties);

            if ( fillGaps ) {
                // The properties we have added may themselves refer to classes in their
                // definitions (for range/domain restrictions); find them now, and add them in
                // the next round
                roundSubset = new HashSet<>();
                for ( OWLAxiom ax : propertyAxioms ) {
                    roundSubset.addAll(ax.getClassesInSignature());
                }
            }

            axioms.addAll(classAxioms);
            axioms.addAll(propertyAxioms);
            added = axioms.size() - size;
            logger.info("subset round {}, added {} axioms", round, added);
            round += 1;

            // If we are not filling gaps, then we can leave after the first round;
            // otherwise, repeat until we cannot find any new axiom to add, indicating that
            // we have reached closure.
        } while ( fillGaps && added > 0 );

        OWLOntology ont = source.getOWLOntologyManager().createOntology();
        source.getOWLOntologyManager().addAxioms(ont, axioms);

        return ont;
    }

    /*
     * Given an initial set of classes, expands it to include all the classes that
     * they refer to, directly or indirectly.
     * 
     * Returns the number of added classes.
     */
    private int makeClassesClosure(Set<OWLClass> subset) {
        Set<OWLClass> tmp = new HashSet<>();
        int added = 0;
        int totalAdded = 0;
        int round = 0;

        do {
            int size = subset.size();
            tmp.clear();

            for ( OWLClass klass : subset ) {
                if ( klass.isBottomEntity() ) {
                    continue;
                }

                // First use the reasoner to get all the superclasses
                for ( OWLClass superClass : reasoner.getSuperClasses(klass, false).getFlattened() ) {
                    if ( includeClass(superClass) ) {
                        tmp.add(superClass);
                        logger.debug("Computing classes closure: adding {} from {}", superClass.getIRI(),
                                klass.getIRI());
                    }
                }

                // Then get classes that are linked by one of the followed properties (or any
                // property). Of note, regardless of any property involved, we never follow (1)
                // disjointness axioms and (2) GCI axioms.
                if ( followAllProperties || !followedProperties.isEmpty() ) {
                    for ( OWLAxiom ax : getAxiomsForClass(klass, false) ) {
                        boolean include = true;
                        if ( ax instanceof OWLDisjointClassesAxiom ) {
                            include = false;
                        } else if ( !followAllProperties ) {
                            // For classes that are part of a class expression, and if the class expression
                            // involves an object property, only include them if the object property is in
                            // the followed set
                            for ( OWLObjectProperty property : ax.getObjectPropertiesInSignature() ) {
                                if ( !followedProperties.contains(property) ) {
                                    include = false;
                                    break;
                                }
                            }
                        }
                        if ( include ) {
                            for ( OWLClass referenced : ax.getClassesInSignature() ) {
                                if ( includeClass(referenced) ) {
                                    tmp.add(referenced);
                                    logger.debug("Computing classes closure: adding {} from {}", referenced.getIRI(),
                                            klass.getIRI());
                                }
                            }
                        }
                    }
                }
            }
            subset.addAll(tmp);
            added = subset.size() - size;
            totalAdded += added;
            round += 1;
            logger.debug("Computing classes closure: round {}: added {} classes", round, added);

            // Repeat until we are no longer finding any new classes
        } while ( added > 0 );
        logger.info("Computing classes closure: added {} classes in {} rounds", totalAdded, round);

        return totalAdded;
    }

    /*
     * Given an initial set of object properties, expands the set to include all the
     * properties that they refer to, directly or not.
     * 
     * Returns the number of added properties.
     */
    private int makeObjectPropertiesClosure(Set<OWLObjectProperty> subset) {
        Set<OWLObjectProperty> tmp = new HashSet<>();
        int added = 0;
        int totalAdded = 0;
        int round = 0;
        boolean reasonerUnsupported = false;

        do {
            int size = subset.size();
            tmp.clear();

            for ( OWLObjectProperty property : subset ) {
                if ( property.isBottomEntity() ) {
                    continue;
                }

                if ( !reasonerUnsupported ) {
                    // Try the reasoner to get the super properties; this may fail as not all
                    // reasoners support this operation (WHELK does not)
                    try {
                        for ( OWLObjectPropertyExpression expr : reasoner.getSuperObjectProperties(property, false)
                                .getFlattened() ) {
                            tmp.addAll(expr.getObjectPropertiesInSignature());
                        }
                    } catch ( UnsupportedOperationException uoe ) {
                        reasonerUnsupported = true;
                    }
                }

                // Peek into the axioms directly - this will get the direct super properties if
                // we couldn't get them from the reasoner, and also the inverse properties
                for ( OWLAxiom ax : source.getAxioms(property, importMode) ) {
                    if ( ax instanceof OWLDisjointObjectPropertiesAxiom ) {
                        continue;
                    }
                    tmp.addAll(ax.getObjectPropertiesInSignature());
                }
            }
            subset.addAll(tmp);
            added = subset.size() - size;
            totalAdded += added;
            round += 1;
            logger.debug("Computing object properties closure: round {}: added {} properties", round, added);

            // Repeat until we are no longer finding any new object properties
        } while ( added > 0 );
        logger.info("Computing object properties closure: added {} properties in {} rounds", totalAdded, round);

        return totalAdded;
    }

    /*
     * Given an initial set of annotation properties, expands it to include all the
     * properties that they refer to, directly or indirectly.
     * 
     * Returns the number of added properties.
     */
    private int makeAnnotationPropertiesClosure(Set<OWLAnnotationProperty> subset) {
        Set<OWLAnnotationProperty> tmp = new HashSet<>();
        int added = 0;
        int totalAdded = 0;
        int round = 0;

        do {
            int size = subset.size();
            tmp.clear();

            for ( OWLAnnotationProperty property : subset ) {
                for ( OWLAxiom ax : source.getAxioms(property, importMode) ) {
                    tmp.addAll(ax.getAnnotationPropertiesInSignature());
                }
                for ( OWLAnnotationAssertionAxiom ax : source.getAnnotationAssertionAxioms(property.getIRI()) ) {
                    tmp.add(ax.getProperty());
                }
            }
            subset.addAll(tmp);
            added = subset.size() - size;
            totalAdded += added;
            logger.debug("Computing annotation properties closure: round {}: added {} properties", round, added);

            // Repeat until we are no longer finding any new annotation properties
        } while ( added > 0 );
        logger.info("Computing annotation properties closure: added {} properties in {} rounds", totalAdded, round);

        return totalAdded;
    }

    /*
     * Fills the provided axioms set with all the axioms that are relevant for the
     * indicated set of classes. For any class, this includes (1) all the “defining”
     * axioms of the class (e.g. SubClassOf, EquivalentClasses, etc.); (2) all GCI
     * axioms referring to the class; (3) all annotation assertions axioms on the
     * class; and (4) the declaration axiom. This excludes any axiom referring to a
     * class that is not in the overall subset (3rd argument).
     */
    private void includeClassAxioms(Set<OWLAxiom> axioms, Set<OWLClass> classes, Set<OWLClass> subset) {
        for ( OWLClass klass : classes ) {
            for ( OWLAxiom ax : getAxiomsForClass(klass, true) ) {
                boolean include = true;
                for ( OWLClass referenced : ax.getClassesInSignature() ) {
                    if ( !subset.contains(referenced) ) {
                        // Exclude any axiom referring to a class outside of the subset
                        include = false;
                        break;
                    }
                }
                if ( include ) {
                    axioms.add(ax);
                }
            }

            axioms.addAll(source.getAnnotationAssertionAxioms(klass.getIRI()));
            axioms.add(factory.getOWLDeclarationAxiom(klass));
        }
    }

    /*
     * Fills the provided axioms set with all the axioms that are relevant for the
     * indicated set of object properties. For any property, this includes (1) all
     * the “defining” axioms; (2) all annotation assertion axioms; and (3) the
     * declaration axiom. This excludes any axiom referring to a property that is
     * not in the subset, or referring to a class that should be excluded for any
     * reason.
     */
    private void includeObjectPropertyAxioms(Set<OWLAxiom> axioms, Set<OWLObjectProperty> properties) {
        for ( OWLObjectProperty property : properties ) {
            for ( OWLAxiom ax : source.getAxioms(property, importMode) ) {
                boolean include = true;
                for ( OWLObjectProperty referenced : ax.getObjectPropertiesInSignature() ) {
                    if ( !properties.contains(referenced) ) {
                        // Exclude any axiom referring to a property outside of the subset
                        include = false;
                        break;
                    }
                }
                if ( include ) {
                    // Exclude any axiom referring to a class that should itself be excluded
                    for ( OWLClass referenced : ax.getClassesInSignature() ) {
                        if ( !includeClass(referenced) ) {
                            include = false;
                            break;
                        }
                    }
                }
                if ( include ) {
                    axioms.add(ax);
                }
            }

            axioms.addAll(source.getAnnotationAssertionAxioms(property.getIRI()));
            axioms.add(factory.getOWLDeclarationAxiom(property));
        }

        // Axioms defining property chains are not included in the set returned by
        // OWLOntology.getAxioms(OWLAnnotationProperty), so we need to get them
        // separately
        for ( OWLSubPropertyChainOfAxiom ax : source.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF, importMode) ) {
            boolean include = true;
            for ( OWLObjectProperty p : ax.getObjectPropertiesInSignature() ) {
                // Only include the chain if it only refers to properties within the subset
                if ( !properties.contains(p) ) {
                    include = false;
                    break;
                }
            }
            if ( include ) {
                axioms.add(ax);
            }
        }
    }

    /*
     * Fills the provided axioms set with all the axioms that are relevant for the
     * indicated set of annotation properties. For any property, this includes (1)
     * the “defining” axioms; (2) all annotation assertion axioms; and (3) the
     * declaration axiom. This excludes any axiom referring to a class that should
     * be excluded for any reason.
     */
    private void includeAnnotationPropertyAxioms(Set<OWLAxiom> axioms, Set<OWLAnnotationProperty> properties) {
        for ( OWLAnnotationProperty property : properties ) {
            for ( OWLAxiom ax : source.getAxioms(property, importMode) ) {
                boolean include = true;
                for ( OWLClass referenced : ax.getClassesInSignature() ) {
                    if ( !includeClass(referenced) ) {
                        include = false;
                        break;
                    }
                }
                if ( include ) {
                    axioms.add(ax);
                }
            }
            axioms.addAll(source.getAnnotationAssertionAxioms(property.getIRI()));
            axioms.add(factory.getOWLDeclarationAxiom(property));
        }
    }

    /*
     * Checks whether a class should be included in the subset.
     */
    private boolean includeClass(OWLClass klass) {
        if ( noDangling && Util.isDangling(source, klass) ) {
            return false;
        }
        if ( includedPrefixes != null || excludedPrefixes != null ) {
            String iri = klass.getIRI().toString();
            if ( includedPrefixes != null ) {
                for ( String prefix : includedPrefixes ) {
                    if ( iri.startsWith(prefix) ) {
                        return true;
                    }
                }
                return false;
            } else {
                for ( String prefix : excludedPrefixes ) {
                    if ( iri.startsWith(prefix) ) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return true;
        }
    }

    /*
     * Helper method to get all the axioms defining a class, including (if the
     * second argument is true) any GCI axiom referring to that class. If GCI axioms
     * are excluded, then this method is exactly equivalent to
     * OWLOntology.getAxioms(klass).
     */
    private Set<OWLAxiom> getAxiomsForClass(OWLClass klass, boolean includeGCIs) {
        Set<OWLAxiom> axioms = new HashSet<>();
        axioms.addAll(source.getAxioms(klass, importMode));
        if ( includeGCIs ) {
            for ( OWLAxiom gca : source.getGeneralClassAxioms() ) {
                if ( gca.containsEntityInSignature(klass) ) {
                    axioms.add(gca);
                }
            }
        }
        return axioms;
    }
}

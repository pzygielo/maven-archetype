/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.archetype.ui;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Iterator;
import java.util.Properties;

import org.apache.maven.archetype.common.Constants;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("default")
@Singleton
public class DefaultArchetypeFactory implements ArchetypeFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultArchetypeFactory.class);

    @Override
    public ArchetypeDefinition createArchetypeDefinition(Properties properties) {
        ArchetypeDefinition definition = new ArchetypeDefinition();

        definition.setGroupId(properties.getProperty(Constants.ARCHETYPE_GROUP_ID));

        definition.setArtifactId(properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID));

        definition.setVersion(properties.getProperty(Constants.ARCHETYPE_VERSION));

        definition.setRepository(properties.getProperty(Constants.ARCHETYPE_REPOSITORY));

        definition.setUrl(properties.getProperty(Constants.ARCHETYPE_URL));

        definition.setDescription(properties.getProperty(Constants.ARCHETYPE_DESCRIPTION));

        return definition;
    }

    private void addOldRequiredProperty(
            ArchetypeConfiguration configuration,
            Properties properties,
            String key,
            String defaultValue,
            boolean initPropertyWithDefault) {
        LOGGER.debug("Adding requiredProperty " + key);

        configuration.addRequiredProperty(key);

        String property = properties.getProperty(key);

        if (property != null) {
            configuration.setProperty(key, property);
            configuration.setDefaultProperty(key, property);
        } else if (defaultValue != null) {
            if (initPropertyWithDefault) {
                configuration.setProperty(key, defaultValue);
            }
            configuration.setDefaultProperty(key, defaultValue);
        }

        LOGGER.debug("Setting property " + key + "=" + configuration.getProperty(key));
    }

    @Override
    @SuppressWarnings("checkstyle:linelength")
    public ArchetypeConfiguration createArchetypeConfiguration(
            org.apache.maven.archetype.old.descriptor.ArchetypeDescriptor archetypeDescriptor, Properties properties) {
        LOGGER.debug("Creating ArchetypeConfiguration from legacy descriptor and Properties");

        ArchetypeConfiguration configuration = createArchetypeConfiguration(properties);

        configuration.setName(archetypeDescriptor.getId());

        addOldRequiredProperty(configuration, properties, Constants.GROUP_ID, null, false);

        addOldRequiredProperty(configuration, properties, Constants.ARTIFACT_ID, null, false);

        addOldRequiredProperty(configuration, properties, Constants.VERSION, "1.0-SNAPSHOT", false);

        addOldRequiredProperty(
                configuration, properties, Constants.PACKAGE, configuration.getProperty(Constants.GROUP_ID), true);

        return configuration;
    }

    private void addRequiredProperty(
            ArchetypeConfiguration configuration,
            Properties properties,
            String key,
            String defaultValue,
            boolean initPropertyWithDefault) {
        if (!configuration.isConfigured(key) && configuration.getDefaultValue(key) == null) {
            addOldRequiredProperty(configuration, properties, key, defaultValue, initPropertyWithDefault);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:linelength")
    public ArchetypeConfiguration createArchetypeConfiguration(
            org.apache.maven.archetype.metadata.ArchetypeDescriptor archetypeDescriptor, Properties properties) {
        LOGGER.debug("Creating ArchetypeConfiguration from fileset descriptor and Properties");

        ArchetypeConfiguration configuration = createArchetypeConfiguration(properties);

        configuration.setName(archetypeDescriptor.getName());

        for (RequiredProperty requiredProperty : archetypeDescriptor.getRequiredProperties()) {
            String key = requiredProperty.getKey();
            LOGGER.debug("Adding requiredProperty " + key);

            configuration.addRequiredProperty(key);

            String defaultValue = requiredProperty.getDefaultValue();
            String validationRegex = requiredProperty.getValidationRegex();

            if (properties.getProperty(key) != null) {
                // using value defined in properties, which overrides any default
                String value = properties.getProperty(key);
                configuration.setProperty(key, value);
                LOGGER.debug("Setting property " + key + "=" + value);
            } else if ((defaultValue != null) && !containsInnerProperty(defaultValue)) {
                // using default value
                configuration.setProperty(key, defaultValue);
                LOGGER.debug("Setting property " + key + "=" + defaultValue);
            }

            if (defaultValue != null) {
                configuration.setDefaultProperty(key, defaultValue);
                LOGGER.debug("Setting defaultProperty " + key + "=" + defaultValue);
            }

            if (validationRegex != null) {
                configuration.setPropertyValidationRegex(key, validationRegex);
                LOGGER.debug("Setting validation regular expression " + key + "=" + defaultValue);
            }
        }

        addRequiredProperty(configuration, properties, Constants.GROUP_ID, null, false);

        addRequiredProperty(configuration, properties, Constants.ARTIFACT_ID, null, false);

        addRequiredProperty(configuration, properties, Constants.VERSION, "1.0-SNAPSHOT", false);

        addRequiredProperty(
                configuration, properties, Constants.PACKAGE, configuration.getProperty(Constants.GROUP_ID), true);

        String postGenerationGoals = properties.getProperty(Constants.ARCHETYPE_POST_GENERATION_GOALS);
        if (postGenerationGoals != null) {
            configuration.setProperty(Constants.ARCHETYPE_POST_GENERATION_GOALS, postGenerationGoals);
        }

        return configuration;
    }

    private void addRequiredProperty(
            ArchetypeConfiguration configuration, Properties properties, String key, String defaultValue) {
        LOGGER.debug("Adding requiredProperty " + key);

        configuration.addRequiredProperty(key);

        if (defaultValue != null) {
            configuration.setDefaultProperty(key, defaultValue);
        }

        if (properties.getProperty(key) != null) {
            configuration.setProperty(key, properties.getProperty(key));

            LOGGER.debug("Setting property " + key + "=" + configuration.getProperty(Constants.GROUP_ID));
        }
    }

    private void setProperty(ArchetypeConfiguration configuration, Properties properties, String key) {
        String property = properties.getProperty(key);

        if (property != null) {
            configuration.setProperty(key, property);
        }
    }

    @Override
    public ArchetypeConfiguration createArchetypeConfiguration(
            MavenProject project, ArchetypeDefinition archetypeDefinition, Properties properties) {
        LOGGER.debug("Creating ArchetypeConfiguration from ArchetypeDefinition, MavenProject and Properties");

        ArchetypeConfiguration configuration = createArchetypeConfiguration(properties);

        for (Iterator<?> requiredProperties = properties.keySet().iterator(); requiredProperties.hasNext(); ) {
            String requiredProperty = (String) requiredProperties.next();

            if (!requiredProperty.contains(".")) {
                LOGGER.debug("Adding requiredProperty " + requiredProperty);
                configuration.addRequiredProperty(requiredProperty);

                configuration.setProperty(requiredProperty, properties.getProperty(requiredProperty));
                LOGGER.debug(
                        "Setting property " + requiredProperty + "=" + configuration.getProperty(requiredProperty));
            }
        }

        addRequiredProperty(configuration, properties, Constants.GROUP_ID, project.getGroupId());

        addRequiredProperty(configuration, properties, Constants.ARTIFACT_ID, project.getArtifactId());

        addRequiredProperty(configuration, properties, Constants.VERSION, project.getVersion());

        addRequiredProperty(configuration, properties, Constants.PACKAGE, null);

        setProperty(configuration, properties, Constants.ARCHETYPE_GROUP_ID);

        setProperty(configuration, properties, Constants.ARCHETYPE_ARTIFACT_ID);

        setProperty(configuration, properties, Constants.ARCHETYPE_VERSION);

        setProperty(configuration, properties, Constants.ARCHETYPE_URL);

        setProperty(configuration, properties, Constants.ARCHETYPE_DESCRIPTION);

        return configuration;
    }

    private ArchetypeConfiguration createArchetypeConfiguration(Properties properties) {
        ArchetypeConfiguration configuration = new ArchetypeConfiguration();

        configuration.setGroupId(properties.getProperty(Constants.ARCHETYPE_GROUP_ID));

        configuration.setArtifactId(properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID));

        configuration.setVersion(properties.getProperty(Constants.ARCHETYPE_VERSION));

        configuration.setUrl(properties.getProperty(Constants.ARCHETYPE_URL));

        configuration.setDescription(properties.getProperty(Constants.ARCHETYPE_DESCRIPTION));

        return configuration;
    }

    @Override
    public void updateArchetypeConfiguration(
            ArchetypeConfiguration archetypeConfiguration, ArchetypeDefinition archetypeDefinition) {
        archetypeConfiguration.setGroupId(archetypeDefinition.getGroupId());
        archetypeConfiguration.setArtifactId(archetypeDefinition.getArtifactId());
        archetypeConfiguration.setVersion(archetypeDefinition.getVersion());
    }

    /**
     * Check if the given value references a property; that is, it contains <code>${...}</code>.
     *
     * @param defaultValue the value to check
     * @return <code>true</code> if the value contains <code>${</code> followed by <code>}</code>
     */
    private boolean containsInnerProperty(String defaultValue) {
        if (defaultValue == null) {
            return false;
        }
        int start = defaultValue.indexOf("${");
        return (start >= 0) && (defaultValue.indexOf("}", start) >= 0);
    }
}

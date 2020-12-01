package com.github.mrcjkb.gradle.modulefinder

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPlugin
import org.gradle.util.VersionNumber

/**
 * Plugin that checks if Jars are modular and if not, adds an Automatic-Module-Name to the MANIFEST if not already present.
 * The Automatic-Module-Name is derived from the Jar file name, in the same manner as the Java module finder derives automatic modules
 * if no Automatic-Module-Name MANIFEST entry is present.
 * This plugin is inspired by the extra-java-module-info by jjohannes: https://github.com/jjohannes/extra-java-module-info
 */
class ModuleFinderPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        if (VersionNumber.parse(target.gradle.gradleVersion) < VersionNumber.parse("6.4-rc-1")) {
            throw IllegalStateException("The module-finder plugin requires Gradle 6.4 or above.")
        }
        target.plugins.withType(JavaPlugin::class.java).configureEach {
            val artifactType: Attribute<String> = Attribute.of("artifactType", String::class.java)
            val javaModule: Attribute<Boolean> = Attribute.of("javaModule", Boolean::class.java)

            // compile and runtime classpath express that they only accept modules by requesting the javaModule=true attribute
            target.configurations.matching { it?.let { isNonTestResolvingJavaPluginConfiguration(it) }?:false }
                    .all { it.attributes.attribute(javaModule, true) }

            // all Jars have a javaModule=false attribute by default; the transform also recognizes modules and returns them without modification
            target.dependencies.artifactTypes.getByName("jar").attributes.attribute(javaModule, false)

            target.dependencies.registerTransform(AutomaticModuleTransform::class.java) {
                it.getFrom().attribute(artifactType, "jar").attribute(javaModule, false)
                it.getTo().attribute(artifactType, "jar").attribute(javaModule, true)
            }
        }
    }

    private fun isNonTestResolvingJavaPluginConfiguration(configuration: Configuration): Boolean {
        return if (!configuration.isCanBeResolved
                || configuration.name.endsWith(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME.substring(1))
                || configuration.name.endsWith(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME.substring(1))
                || configuration.name.endsWith(JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME.substring(1))) {
            false
        } else configuration.name.endsWith(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME.substring(1))
                || configuration.name.endsWith(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME.substring(1))
                || configuration.name.endsWith(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.substring(1))
    }

}
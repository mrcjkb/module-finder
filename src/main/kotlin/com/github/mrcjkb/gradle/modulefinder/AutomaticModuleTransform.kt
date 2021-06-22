package com.github.mrcjkb.gradle.modulefinder

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.regex.Pattern

/**
 * Artifact transform that checks if Jars are modular and if not, adds an Automatic-Module-Name to the MANIFEST if not already present.
 * The Automatic-Module-Name is derived from the Jar file name, in the same manner as the Java module finder derives automatic modules
 * if no Automatic-Module-Name MANIFEST entry is present.
 * This plugin is inspired by the extra-java-module-info by jjohannes: https://github.com/jjohannes/extra-java-module-info
 */
abstract class AutomaticModuleTransform: TransformAction<TransformParameters.None> {

    @InputArtifact
    protected abstract fun getInputArtifact(): Provider<FileSystemLocation?>?

    override fun transform(outputs: TransformOutputs) {
        getInputArtifact()?.get()?.asFile?.let { inJar ->
            if (isModule(inJar)) {
                outputs.file(inJar)
                return
            }
            with(outputs.file(inJar.name.substring(0, inJar.name.lastIndexOf('.')) + "-module.jar")) {
                addAutomaticModuleName(inJar, this, deriveModuleName(inJar.nameWithoutExtension))
            }
        }
    }

    private fun isModule(jarFile: File): Boolean {
        JarInputStream(FileInputStream(jarFile)).use {
            val isMultiReleaseJar = it.manifest?.mainAttributes?.getValue("Multi-Release")?.toBoolean()?:false
            var next = it.nextEntry
            while (next != null) {
                if ("module-info.class" == next.name) {
                    return true
                }
                if (isMultiReleaseJar && Pattern.compile("META-INF/versions/\\d+/module-info.class").matcher(next.name).matches()) {
                    return true
                }
                next = it.nextEntry
            }
            return it.manifest?.mainAttributes?.getValue("Automatic-Module-Name") != null
        }
    }

    private fun addAutomaticModuleName(originalJar: File, moduleJar: File, moduleName: String) {
        JarInputStream(FileInputStream(originalJar)).use { inputStream ->
            val manifest = inputStream.manifest ?: run {
                val manifest = Manifest()
                manifest.mainAttributes.putValue("Manifest-Version", "1.0")
                return@run manifest
            }
            manifest.mainAttributes.putValue("Automatic-Module-Name", moduleName)
            val exclude = Regex("^META-INF/[^/]+\\.(SF|RSA|DSA|sf|rsa|dsa)$")
            JarOutputStream(FileOutputStream(moduleJar), manifest).use { outputStream ->
                var jarEntry = inputStream.nextJarEntry
                while (jarEntry != null) {
                    if (!exclude.matches(jarEntry.name)) {
                        outputStream.putNextEntry(jarEntry)
                        outputStream.write(inputStream.readAllBytes())
                        outputStream.closeEntry()
                    }
                    jarEntry = inputStream.nextJarEntry
                }
            }
        }
    }

}
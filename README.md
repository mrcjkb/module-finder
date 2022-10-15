# Gradle module-finder plugin
Gradle plugin that enables the use of non-modular Java dependencies without an "Automatic Module Name" in their manifest.

> [![Maintenance](https://img.shields.io/badge/Maintained%3F-no-red.svg)](https://bitbucket.org/lbesson/ansi-colors) 
>
> __NOTE:__
>
>  Since I hardly work with Java these days, I will no longer be actively maintaining this plugin.
> I will gladly review PRs, but if you want a more actively maintained alternative,
> I recommend the [extra-java-module-info](https://github.com/gradlex-org/extra-java-module-info) plugin.

# Latest release
The latest release is available on the [Gradle plugin page](https://plugins.gradle.org/plugin/com.github.mrcjkb.module-finder).

# Requires
- Gradle 6.4+

# Description
[As of version 6.7.1, Gradle puts traditional libraries that provide no module information at all on the `classpath`, instead of the `module path`.](https://docs.gradle.org/current/userguide/java_library_plugin.html#using_libraries_that_are_not_modules).
This is inconsistent with the behaviour of the Java module finder, which automatically derives a module name from the JAR file name, if it has no "Automatic-Module-Name" attribute in its MANIFEST.

As such, modular projects that use non-modular dependencies without an "Automatic Module Name" attribute in their MANIFEST will fail to compile in Gradle, as the classpath cannot be accessed from named modules.
This plugin fixes that issue, by adding a [transform](https://docs.gradle.org/current/userguide/artifact_transforms.html) to each affected dependency, which derives an automatic module name based on the Jar file name.

The Plugin performs the following checks, to ensure all non-test dependencies are added to the `module path`:

- A JAR file with a `module-info.class` in its top-level directory, or in a versioned entry in a multi-release JAR file, is a modular JAR file and thus already defines an explicit module.

- If the JAR file has the attribute “Automatic-Module-Name" in its main MANIFEST then it is an automatic module and treated as such by Gradle.

If none of the above apply, this plugin performs a [transform](https://docs.gradle.org/current/userguide/artifact_transforms.html) and adds an "Automatic Module Name" attribute, with its value derived from the JAR file name, as follows:

- The “.jar" suffix is removed.
- If the name matches the regular expression "-(\\d+(\\.|$))" then the module name will be derived from the subsequence preceding the hyphen of the first occurrence.
- All non-alphanumeric characters ([^A-Za-z0-9]) in the module name are replaced with a dot (".").
- All repeating dots are replaced with one dot.
- All leading and trailing dots are removed.

As an example, a JAR file named “foo-bar-1.2.3.jar" will derive a module name "foo.bar".

# Usage
Simply apply the plugin to your Gradle build file. The `java-library` plugin is applied automatically.

# Credits
This plugin is inspired by the [extra-java-module-info plugin version 0.3 by jjohannes](https://github.com/jjohannes/extra-java-module-info), licensed under the [Apache License 2.0](https://github.com/jjohannes/extra-java-module-info/blob/master/LICENSE), &copy; 2020

package com.github.mrcjkb.gradle.modulefinder

import java.io.File

fun deriveModuleName(jarName: String): String {
    val jarNameWithoutExtension = File(jarName).nameWithoutExtension
    return "-(\\d+(\\.|$))*".toRegex().replace(jarNameWithoutExtension, ".")
        .replace("[^A-Za-z0-9]".toRegex(), ".")
        .replace("\\.+".toRegex(), ".")
        .trim('.')
}
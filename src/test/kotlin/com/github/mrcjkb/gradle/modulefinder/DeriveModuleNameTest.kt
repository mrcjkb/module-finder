package com.github.mrcjkb.gradle.modulefinder

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class DeriveModuleNameTest: StringSpec({

    "foo_bar.jar should be derived as foo.bar" {
        deriveModuleName("foo_bar.jar") shouldBe "foo.bar"
    }

    "foo-bar.jar should be derived as foo.bar" {
        deriveModuleName("foo-bar.jar") shouldBe "foo.bar"
    }

    "foo-bar-1.4.21.jar should be derived as foo.bar" {
        deriveModuleName("foo-bar-1.4.21.jar") shouldBe "foo.bar"
    }

    "foo_bar-1.4.21.jar should be derived as foo.bar" {
        deriveModuleName("foo_bar-1.4.21.jar") shouldBe "foo.bar"
    }

})
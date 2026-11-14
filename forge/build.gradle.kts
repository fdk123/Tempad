architectury {
    forge()
}

loom {
    forge {
        mixinConfig("tempad-common.mixins.json")
        mixinConfig("tempad.mixins.json")
    }
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentForge"].extendsFrom(this)
}

dependencies {
    common(project(":common", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(path = ":common", configuration = "transformProductionForge")) {
        isTransitive = false
    }

    val minecraftVersion: String by project
    val forgeVersion: String by project
    val curiosVersion: String by project
    val jadeVersion: String by project

    forge(group = "net.neoforged", name = "forge", version = "$minecraftVersion-$forgeVersion")

    "modRuntimeOnly"("top.theillusivec4.curios:curios-forge:${curiosVersion}")
    "modCompileOnly"("top.theillusivec4.curios:curios-forge:${curiosVersion}:api")
    "modCompileOnly"("maven.modrinth:jade:$jadeVersion") { isTransitive = false }
    "modCompileOnly"("earth.terrarium.cadmus:cadmus-forge-1.20.1:1.0.8") { isTransitive = false }
    "modCompileOnly"("earth.terrarium.argonauts:argonauts-forge-1.20.1:1.0.10") { isTransitive = false }
}

package me.leolee.generator

import me.leolee.annotation.TSEnum
import me.leolee.annotation.TSInterface
import me.leolee.scanner.SpringAppFileScanner
import me.ntrrgc.tsGenerator.ClassTransformer
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import java.io.File
import kotlin.reflect.KClass


/*
* This class is a scanner with a few additional niceties
* 1. Scans the java and kotlin files in a specified src folder
* 2. Hashes the file names that are marked with an annotation (@TSInterface, @TSEnum respectively)
* 3. Compares the hashes to see if a new set of ts.def files need to be generated
* 4. If the hashes are different execute the generator on those files and create a ts.definition file
* */
class TypeScriptGeneratorByAnnotation {

    companion object Companion {
        private fun generateDefinitionFile(
                classes: List<KClass<*>>,
                overrideMappings: Map<KClass<*>, String>,
                classTransformers: List<ClassTransformer>,
                ignoreSuperclasses: Set<KClass<*>>): String {

            return TypeScriptGenerator(
                    rootClasses = classes,
                    mappings = overrideMappings,
                    classTransformers = classTransformers,
                    ignoreSuperclasses = ignoreSuperclasses
            ).definitionsText
        }

        fun createFileFromGeneratedDefinitionFiles(
                bundleName: String,
                bundleDestinationPath: String,
                rootClassPaths: List<String>,
                classTransformers: List<ClassTransformer> = emptyList()) {

            val classes = mutableListOf<KClass<*>>()
            val overriddenClasses = mutableMapOf<KClass<*>, String>()
            val ignoreSuperclasses = mutableSetOf<KClass<*>>()

            val scanner = SpringAppFileScanner()

            for (rootClassPath in rootClassPaths) {
                val classesToAdd = scanner.scanFromBasePackage(rootClassPath)
                classes.addAll(classesToAdd)

                overriddenClasses.putAll(generateOverriddenClasses(classesToAdd))
                ignoreSuperclasses.addAll(generateIgnoreSuperclasses(classesToAdd))

            }
            val typeDefStr = generateDefinitionFile(classes, overriddenClasses, classTransformers, ignoreSuperclasses)

            // TODO IO
            if (resolvePath(bundleDestinationPath)) {
                writeFile(bundleName, bundleDestinationPath, typeDefStr)
            }
        }

        fun createFileFromRootDirectory(
            bundleName: String,
            bundleDestinationPath: String,
            rootDirectoryPath: String) {

            //createFileFromGeneratedDefinitionFiles()
        }

        private fun generateOverriddenClasses(kClassList: List<KClass<*>>): Map<KClass<*>, String> {

            val map = mutableMapOf<KClass<*>, String>()
            for (kClass in kClassList) {
                kClass.annotations
                        .filter { it is TSEnum || it is TSInterface }
                        .first()
                        .let {
                            var overrideClass: String = ""

                            if (it is TSInterface && it.overrideClassName.isNotBlank()) {
                                overrideClass = it.overrideClassName
                            } else if (it is TSEnum && it.overrideClassName.isNotBlank()) {
                                overrideClass = it.overrideClassName
                            }

                            map.put(kClass, overrideClass)
                        }
            }

            return map
        }

        private fun generateIgnoreSuperclasses(kClassList: List<KClass<*>>): List<KClass<*>> {

            val list = mutableListOf<KClass<*>>()
            for (kClass in kClassList) {
                kClass.annotations
                        .filter { it is TSInterface }
                        .first()
                        .let {
                            if (it is TSInterface && it.ignoreSuper) {
                                list.add(kClass)
                            }
                        }
            }

            return list
        }

        // Resolve paths, handle OS.
        private fun generateFilePath(bundleName: String, bundleDestinationPath: String): String {
            var path = bundleDestinationPath
            if (bundleDestinationPath.subSequence(bundleDestinationPath.length - 2..bundleDestinationPath.length)
                            .toString() == "\\\\") {
                path += "\\\\"

            }
            else if (bundleDestinationPath.last() != '/') {
                path += "/"
            }

            return "$path$bundleName.d.ts"
        }

        // Write File
        private fun writeFile(bundleName: String, bundleDestinationPath: String, fileText: String) {
            val fileRef = File(generateFilePath(bundleName, bundleDestinationPath))

            if (fileRef.isFile && fileRef.canRead()) {
                fileRef.delete()
            }

            fileRef.createNewFile()
            fileRef.writeText(fileText)
        }

        // Short circuit if the directory exists, else generate the necessary directories
        private fun resolvePath(bundleDestinationPath: String): Boolean {
            return File(bundleDestinationPath).isDirectory || File(bundleDestinationPath).mkdirs()
        }
    }
}
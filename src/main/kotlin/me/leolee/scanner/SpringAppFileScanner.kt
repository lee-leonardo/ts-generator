package me.leolee.scanner

import me.leolee.annotation.TSEnum
import me.leolee.annotation.TSInterface
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.util.ClassUtils
import org.springframework.util.SystemPropertyUtils
import kotlin.reflect.KClass

class SpringAppFileScanner private constructor(
        val checkCandidate: (candidate: KClass<*>) -> Boolean
) {
    constructor(): this(defaultCandidateChecker())

    fun scanFromBasePackage(basePackage: String): List<KClass<*>> {
        val resourcePatternResolver = PathMatchingResourcePatternResolver()
        val metadataReaderFactory = CachingMetadataReaderFactory(resourcePatternResolver)

        val classCandidates = mutableListOf<KClass<*>>()
        val packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + "/" + "**/*.class"

        val resources = resourcePatternResolver.getResources(packageSearchPath)

        for (resource in resources) {
            if (resource.isReadable) {
                val metadataReader = metadataReaderFactory.getMetadataReader(resource)
                if (isCandidate(metadataReader)) {
                    classCandidates.add(Class.forName(metadataReader.classMetadata.className)::class)
                }
            }
        }

        return classCandidates
    }

    private fun resolveBasePackage(basePackage: String) =
            ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage))

    private fun isCandidate(metadataReader: MetadataReader): Boolean {
        Class.forName(metadataReader.classMetadata.className)::class.let {
            return checkCandidate(it)
        }
    }

    companion object Builder {
        fun defaultCandidateChecker() = { candidate: KClass<*> -> candidate.annotations
                    .filter { it == TSInterface.asAnnotation() || it == TSEnum.asAnnotation() }
                    .isNotEmpty()
        }
    }
}
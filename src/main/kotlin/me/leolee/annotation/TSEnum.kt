package me.leolee.annotation

import kotlin.reflect.KClass

annotation class TSEnum {
    companion object {
        fun asAnnotation(): KClass<*> {
            return TSEnum::class
        }
    }
}
package me.leolee.annotation

import kotlin.reflect.KClass

/*
* A target for a more nuanced generation of type definition files.
* Users don't necessarily want to expose object that are used internally.
* */
annotation class TSInterface {
    companion object {
        fun asAnnotation(): KClass<*> {
            return TSEnum::class
        }
    }
}
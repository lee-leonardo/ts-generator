package me.leolee.annotation

import kotlin.reflect.KClass

/*
* A target for a more nuanced generation of type definition files.
* Users don't necessarily want to expose object that are used internally.
* */
annotation class TSInterface(
        val overrideClassName: String = "",
        val ignoreSuper: Boolean = false
)
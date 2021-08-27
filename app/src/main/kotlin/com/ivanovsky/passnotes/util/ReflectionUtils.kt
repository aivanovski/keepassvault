package com.ivanovsky.passnotes.util

object ReflectionUtils {

    @JvmStatic
    fun containsInterfaceInClass(classType: Class<*>, interfaceType: Class<*>): Boolean {
        return classType.interfaces
            .any { classInterface -> classInterface == interfaceType }
    }

    fun getClassByName(typeName: String): Class<*>? {
        return try {
            Class.forName(typeName)
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}
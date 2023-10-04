package me.rhunk.snapenhance.mapper.impl

import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.MapperContext
import me.rhunk.snapenhance.mapper.ext.isAbstract

class DefaultMediaItemMapper : AbstractClassMapper() {
    override fun run(context: MapperContext) {
        for (clazz in context.classes) {
            val superClass = context.getClass(clazz.superclass) ?: continue

            if (!superClass.isAbstract() || superClass.interfaces.isEmpty() || superClass.interfaces[0] != "Ljava/lang/Comparable;") continue
            if (clazz.methods.none { it.returnType == "Landroid/net/Uri;" }) continue

            val constructorParameters = clazz.directMethods.firstOrNull { it.name == "<init>" }?.parameterTypes ?: continue
            if (constructorParameters.size < 6 || constructorParameters[5] != "J") continue

            context.addMapping("DefaultMediaItem", clazz.type.replace("L", "").replace(";", ""))
            return
        }
    }
}
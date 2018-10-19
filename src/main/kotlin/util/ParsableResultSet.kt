package util

import java.io.InputStream
import java.sql.Blob
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

interface ParsableResultSet {
    fun <T : DTO> ResultSet.parseList(clazz: KClass<T>): List<T>? {
        val list = arrayListOf<T>()
        val columns = clazz.memberProperties
        val constructor = clazz.primaryConstructor ?: throw Exception("There is no primary constructor")
        val cParameters = constructor.parameters

        val map = hashMapOf<KClass<*>, String>(
                Int::class to "getInt",
                Byte::class to "getByte",
                Long::class to "getLong",

                Float::class to "getFloat",
                Double::class to "getDouble",

                Boolean::class to "getBoolean",
                ByteArray::class to "getBytes",
                InputStream::class to "getBinaryStream",
                String::class to "getString",

                Date::class to "getDate",
                Time::class to "getTime",
                Blob::class to "getBlob"
        )

        while (this.next()) {
            val parameters = mutableMapOf<KParameter, Any>()

            columns.forEach { col ->
                map.forEach { kClass, matchFun ->
                    if (subTypeChk(col, kClass)) {
                        val key = cParameters.first { it.name == col.name }

                        val value = this::class.memberFunctions
                                .filter { it.name == matchFun }
                                .first { kFun ->
//                                    kFun.parameters.forEach { System.out.println("index ${it.index} name ${it.name} type ${it.type} kind ${it.kind}") }
//                                    parameters[1] == rs::this
                                    kFun.parameters.any { it.type.isSubtypeOf(String::class.createType()) }
                                }
                                .call(this, col.name)

                        parameters[key] = kClass.cast(value)
                    }
                }
            }

            list.add(constructor.callBy(parameters))
        }

        return if (list.isEmpty()) null else list
    }

    fun <T : DTO> subTypeChk(prop: KProperty1<T, *>, clazz: KClass<*>) = prop.returnType.isSubtypeOf(clazz.createType())
}
package util

import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

interface ParsableResultSet {
    fun <T : DTO> ResultSet.parseList(clazz: KClass<T>): List<T>? {
        val list = arrayListOf<T>()
        val columns: Collection<KProperty1<T, *>> = clazz.memberProperties
        val constructor: KFunction<T> = clazz.primaryConstructor ?: throw Exception("There is no primary constructor")
        val cParameters: List<KParameter> = constructor.parameters

        val map = hashMapOf<KClass<*>, String>(
                Int::class to "getInt",
                Byte::class to "getByte",
                Long::class to "getLong",

                Float::class to "getFloat",
                Double::class to "getDouble",

                Any::class to "getObject",
                String::class to "getString",
                Boolean::class to "getBoolean",
                ByteArray::class to "getBytes",

                java.sql.Date::class to "getDate",
                java.sql.Time::class to "getTime",
                java.sql.Blob::class to "getBlob",
                java.sql.Clob::class to "getClob",
                java.io.Reader::class to "getCharacterStream",
                java.io.InputStream::class to "getBinaryStream"
        )

        while (this.next()) {
            val parameters = mutableMapOf<KParameter, Any>()

     /*       columns.forEach { col ->
                map.forEach { kClass, matchFun ->
                    if (subTypeChk(col, kClass)) {
                        val key = cParameters.first { it.name == col.name }

                        val value = ResultSet::class.memberFunctions
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
            }*/

            columns.parallelStream().forEach {col ->
                map.filter { e -> subTypeChk(col,e.key) }.forEach { kClass, matchFun ->
                    val key = cParameters.first { it.name == col.name }

                    val value = ResultSet::class.memberFunctions
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

            list.add(constructor.callBy(parameters))
        }

        return if (list.isEmpty()) null else list
    }

    fun <T : DTO> subTypeChk(prop: KProperty1<T, *>, clazz: KClass<*>) = prop.returnType.isSubtypeOf(clazz.createType())
}
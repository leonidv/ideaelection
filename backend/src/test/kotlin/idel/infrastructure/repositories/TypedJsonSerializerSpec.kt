package idel.infrastructure.repositories

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.java.codec.TypeRef
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec

class Person(val name: String, val age: Int)

class PersonTypeRef() : TypeRef<Person>()

class TypedJsonSerializerSpec : DescribeSpec({
    val objectMapper = jacksonObjectMapper()
    val serializer = TypedJsonSerializer(objectMapper, "person", "test", Person::class.java)
    
    describe("serialize") {
        describe("serializer Person(name=[a],age=[10])") {
            val p = Person("a", 10)
            val result = serializer.serialize(p)
            var resultAsString = String(result)
            val expected = """{"_type":"test","name":"a","age":10}"""
            it("result as UTF8 string is [$expected]") {
                assertThat(resultAsString).isEqualTo(expected)
            }
        }
    }

    val jsonStr = """{"name":"a","age":10,"_type":"test"}"""
    val incorrectTypeJsonStr = """{"name":"a","age":10,"_type":"incorrect"}"""
    describe("deserialize to class"){

        describe("json has correct type, json=[$jsonStr]") {
            val p = serializer.deserialize(Person::class.java, jsonStr.toByteArray())
            it("name is [a]") {
                assertThat(p.name).isEqualTo("a")
            }

            it("age is [10]") {
                assertThat(p.age).isEqualTo(10)
            }
        }

        describe("json has incorrect type, json=[$incorrectTypeJsonStr]") {
            it("throw exception UnexpectedTypeDecodingFailure") {
                assertThat{
                    serializer.deserialize(Person::class.java, incorrectTypeJsonStr.toByteArray())
                }.isFailure().hasClass(UnexpectedTypeDecodingFailure::class.java)

            }
        }

        describe("json is wrapped") {
            val s = """{"person":$jsonStr}"""
            val p = serializer.deserialize(Person::class.java, s.toByteArray())

        }
    }



//    xdescribe("deserialize to TypeRef","method is not implemented") {
//        describe("json has correct type, json=[$jsonStr]") {
//            val p = serializer.deserialize(PersonTypeRef(),jsonStr.toByteArray())
//            it ("name is [a]") {
//                assertThat(p.name).isEqualTo("a")
//            }
//
//            it("age is [10]") {
//                assertThat(p.age).isEqualTo(10)
//            }
//        }
//
//        it("json has incorrect type, json=[$incorrectTypeJsonStr]") {
//            assertThat{
//                serializer.deserialize(PersonTypeRef(),incorrectTypeJsonStr.toByteArray())
//            }.isFailure().hasClass(DecodingFailureException::class.java)
//        }
//    }
})
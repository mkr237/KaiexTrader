package kaiex.util

import java.nio.ByteOrder
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


/**
 * This class contains static methods that leverage [java.util.UUID] and
 * [java.security.MessageDigest] to create version-5 UUIDs with full
 * namespace support.
 *
 *
 * The UUID class provided by java.util is suitable as a datatype for UUIDs of
 * any version, but lacks methods for creating version 5 (SHA-1 based) UUIDs.
 * Its implementation of version 3 (MD5 based) UUIDs also lacks build-in
 * namespace support.
 *
 *
 * This class was informed by [RFC
 * 4122](http://www.ietf.org/rfc/rfc4122.txt). Since RFC 4122 is vague on how a 160-bit hash is turned into the
 * 122 free bits of a UUID (6 bits being used for version and variant
 * information), this class was modelled after java.util.UUID's type-3
 * implementation and validated against the D language's phobos library [std.uuid](http://dlang.org/phobos/std_uuid.html), which in turn was
 * modelled after the Boost project's [boost.uuid](http://www.boost.org/doc/libs/1_42_0/libs/uuid/uuid.html); and also validated against the Python language's [uuid](http://docs.python.org/2/library/uuid.html) library.
 *
 * @see java.util.UUID
 *
 * @see java.security.MessageDigest
 *
 *
 * @author Luther Tychonievich. Released into the public domain. I would
 * consider it a courtesy if you cite me if you benefit from this code.
 */
object UUID5 {
    /**
     * The following namespace is a type-5 UUID of "polygenea" in the null
     * namespace
     */
    val POLYGENEA_NAMESPACE = UUID.fromString("954aac7d-47b2-5975-9a80-37eeed186527")

    /**
     * Similar to UUID.nameUUIDFromBytes, but does version 5 (sha-1) not version
     * 3 (md5)
     *
     * @param name
     * The bytes to use as the "name" of this hash
     * @return the UUID object
     */
    fun fromBytes(name: ByteArray?): UUID {
        if (name == null) {
            throw NullPointerException("name == null")
        }
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            makeUUID(md.digest(name), 5)
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
    }

    /**
     * Similar to UUID.nameUUIDFromBytes, but does version 5 (sha-1) not version
     * 3 (md5) and uses a namespace
     *
     * @param namespace
     * The namespace to use for this UUID. If null, uses
     * 00000000-0000-0000-0000-000000000000
     * @param name
     * The bytes to use as the "name" of this hash
     * @return the UUID object
     */
    fun fromBytes(namespace: UUID?, name: ByteArray?): UUID {
        if (name == null) {
            throw NullPointerException("name == null")
        }
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            if (namespace == null) {
                md.update(ByteArray(16))
            } else {
                md.update(asBytes(namespace.mostSignificantBits, ByteOrder.BIG_ENDIAN))
                md.update(asBytes(namespace.leastSignificantBits, ByteOrder.BIG_ENDIAN))
            }
            makeUUID(md.digest(name), 5)
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
    }

    /**
     * Similar to UUID.nameUUIDFromBytes, but does version 5 (sha-1) not version
     * 3 (md5)
     *
     * @param name
     * The string to be encoded in utf-8 to get the bytes to hash
     * @return the UUID object
     */
    fun fromUTF8(name: String): UUID {
        return fromBytes(name.toByteArray(Charset.forName("UTF-8")))
    }

    /**
     * Similar to UUID.nameUUIDFromBytes, but does version 5 (sha-1) not version
     * 3 (md5) and uses a namespace
     *
     * @param namespace
     * The namespace to use for this UUID. If null, uses
     * 00000000-0000-0000-0000-000000000000
     * @param name
     * The string to be encoded in utf-8 to get the bytes to hash
     * @return the UUID object
     */
    fun fromUTF8(namespace: UUID?, name: String): UUID {
        return fromBytes(namespace, name.toByteArray(Charset.forName("UTF-8")))
    }

    /**
     * A helper method for making uuid objects, which in java store longs not
     * bytes
     *
     * @param src
     * An array of bytes having at least offset+8 elements
     * @param offset
     * Where to start extracting a long
     * @param order
     * either ByteOrder.BIG_ENDIAN or ByteOrder.LITTLE_ENDIAN
     * @return a long, the specified endianness of which matches the bytes in
     * src[offset,offset+8]
     */
    fun peekLong(src: ByteArray, offset: Int, order: ByteOrder): Long {
        var ans: Long = 0
        if (order == ByteOrder.BIG_ENDIAN) {
            var i = offset
            while (i < offset + 8) {
                ans = ans shl 8
                ans = ans or (src[i].toLong() and 0xffL)
                i += 1
            }
        } else {
            var i = offset + 7
            while (i >= offset) {
                ans = ans shl 8
                ans = ans or (src[i].toLong() and 0xffL)
                i -= 1
            }
        }
        return ans
    }

    /**
     * A helper method for writing uuid objects, which in java store longs not
     * bytes
     *
     * @param data
     * A long to write into the dest array
     * @param dest
     * An array of bytes having at least offset+8 elements
     * @param offset
     * Where to start writing a long
     * @param order
     * either ByteOrder.BIG_ENDIAN or ByteOrder.LITTLE_ENDIAN
     */
    fun putLong(data: Long, dest: ByteArray, offset: Int, order: ByteOrder) {
        var data = data
        if (order == ByteOrder.BIG_ENDIAN) {
            var i = offset + 7
            while (i >= offset) {
                dest[i] = (data and 0xffL).toByte()
                data = data shr 8
                i -= 1
            }
        } else {
            var i = offset
            while (i < offset + 8) {
                dest[i] = (data and 0xffL).toByte()
                data = data shr 8
                i += 1
            }
        }
    }

    /**
     * A helper method for reading uuid objects, which in java store longs not
     * bytes
     *
     * @param data
     * a long to convert to bytes
     * @param order
     * either ByteOrder.BIG_ENDIAN or ByteOrder.LITTLE_ENDIAN
     * @return an array of 8 bytes
     */
    fun asBytes(data: Long, order: ByteOrder): ByteArray {
        val ans = ByteArray(8)
        putLong(data, ans, 0, order)
        return ans
    }

    /**
     * A private method from UUID pulled out here so we have access to it.
     *
     * @param hash
     * A 16 (or more) byte array to be the basis of the UUID
     * @param version
     * The version number to replace 4 bits of the hash (the variant
     * code will replace 2 more bits))
     * @return A UUID object
     */
    fun makeUUID(hash: ByteArray, version: Int): UUID {
        var msb = peekLong(hash, 0, ByteOrder.BIG_ENDIAN)
        var lsb = peekLong(hash, 8, ByteOrder.BIG_ENDIAN)
        // Set the version field
        msb = msb and (0xfL shl 12).inv()
        msb = msb or (version.toLong() shl 12)
        // Set the variant field to 2
        lsb = lsb and (0x3L shl 62).inv()
        lsb = lsb or (2L shl 62)
        return UUID(msb, lsb)
    }
}
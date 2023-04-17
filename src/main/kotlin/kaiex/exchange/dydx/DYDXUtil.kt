package kaiex.exchange.dydx

import com.fersoft.signature.Signature
import kaiex.util.UUID5
import kotlinx.serialization.json.Json
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString as myJsonEncode


// for UUID5
val NAMESPACE: UUID = UUID.fromString("0f9da948-a6fb-4c45-9edc-4685c3f3317d")

//fun convert(sig: StarknetCurveSignature): String {
//    return String.format("%1$64s%2$64s", sig.r.value.toString(16), sig.s.value.toString(16)).replace(" ", "0")
//}

fun deriveFinalSignature(sig: Signature): String {
    return String.format("%1$64s%2$64s", sig.r.toString(16), sig.s.toString(16)).replace(" ", "0")
}

fun sign(
    requestPath: String,
    method: String,
    isoTimestamp: String,
    data: Map<String, String>
): String {

    val DYDX_API_SECRET = System.getenv("DYDX_API_SECRET")
    val messageString = isoTimestamp + method + requestPath + if (data.isNotEmpty()) jsonStringify(data) else ""
    val hmac = Mac.getInstance("HmacSHA256")
    val secret = SecretKeySpec(
        Base64.getUrlDecoder().decode(encodeUtf8(DYDX_API_SECRET)),
        "HmacSHA256"
    )
    hmac.init(secret)
    val digest = hmac.doFinal(messageString.toByteArray())

    return Base64.getUrlEncoder().encodeToString(digest)
}

fun jsonStringify(obj: Map<String, String>): String {
    val json = Json { encodeDefaults = true }
    return json.myJsonEncode(obj)
}

fun encodeUtf8(str: String): ByteArray {
    return str.toByteArray(charset("UTF-8"))
}

fun getAccountId(address: String, accountNumber: Int = 0): String {
    val userId = UUID5.fromUTF8(NAMESPACE, address.lowercase()).toString()
    return UUID5.fromUTF8(NAMESPACE, userId + accountNumber.toString()).toString()
}

fun nowISO(): String {
    val tz = TimeZone.getTimeZone("UTC")
    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    df.timeZone = tz
    return df.format(Date())
}

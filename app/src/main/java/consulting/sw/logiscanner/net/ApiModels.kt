package consulting.sw.logiscanner.net

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Credentials(
    val Email: String,
    val Password: String
)

@JsonClass(generateAdapter = true)
data class ErrMessage(
    val Msg: String
)

@JsonClass(generateAdapter = true)
data class UserViewItemWithJWT(
    val Id: Int,
    val FirstName: String,
    val LastName: String,
    val Patronymic: String?,
    val Email: String,
    val Roles: List<String>,
    val Token: String
)

@JsonClass(generateAdapter = true)
data class ScanCheckRequest(
    val code: String
)

@JsonClass(generateAdapter = true)
data class ScanCheckResponse(
    val match: Boolean
)

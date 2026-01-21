package consulting.sw.logiscanner.repo

import android.content.Context
import consulting.sw.logiscanner.net.Credentials
import consulting.sw.logiscanner.net.NetworkModule
import consulting.sw.logiscanner.net.UserViewItemWithJWT
import consulting.sw.logiscanner.store.AuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class LoginRepository(context: Context) {

    private val _state = MutableStateFlow(UserViewItemWithJWT(-1, "", "", "", "", emptyList(), ""))
    val state = _state.asStateFlow()

    val token: String? get() = state.value.Token.takeIf { it.isNotBlank() }

    private val store = AuthStore(context)

    init {
        runBlocking {
            val stored = store.get().first()
            if (stored.token?.isNotBlank() == true) {
                _state.update {
                    it.copy(
                        Token = stored.token!!,
                        FirstName = stored.firstName!!,
                        LastName = stored.lastName!!,
                        Id = stored.id!!
                    )
                }
            }
        }
    }

    suspend fun login(baseUrl: String, email: String, pass: String) {
        val api = NetworkModule.createApi(baseUrl)
        val res = api.login(Credentials(email, pass))

        val body = res
        store.save(body.Token, body.FirstName, body.LastName, body.Id)
        _state.update {
            it.copy(
                Token = body.Token,
                FirstName = body.FirstName,
                LastName = body.LastName,
                Id = body.Id
            )
        }
    }

    suspend fun logout() {
        store.clear()
        _state.update {
            it.copy(Token = "", FirstName = "", LastName = "", Id = -1)
        }
    }
}

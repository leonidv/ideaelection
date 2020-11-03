package idel.tests.infrastructure

import java.net.Authenticator
import java.net.PasswordAuthentication

class IdelHttpAuthenticator(val username: String, val password: String = username) : Authenticator() {

    override fun getPasswordAuthentication(): PasswordAuthentication =
            PasswordAuthentication(username, password.toCharArray())

}
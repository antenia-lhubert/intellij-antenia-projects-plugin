package fr.antenia.database

import fr.antenia.config.OrderedPropertiesCodec
import fr.antenia.credentials.DatabaseCredentials
import fr.antenia.project.DatabaseKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MysqlConnectionTest {
    @Test
    fun `parses mysql connection details`() {
        assertEquals(
            MysqlConnection("mysql.example.com", 3307, "neo", "?autoReconnect=true"),
            MysqlConnection.parse("jdbc:mysql://mysql.example.com:3307/neo?autoReconnect=true"),
        )
    }

    @Test
    fun `defaults mysql port and rebuilds url`() {
        val connection = MysqlConnection.parse("jdbc:mysql://mysql.example.com/neo")

        assertEquals(3306, connection?.port)
        assertEquals("jdbc:mysql://mysql.example.com/neo", connection?.url)
    }

    @Test
    fun `rejects non mysql urls`() {
        assertNull(MysqlConnection.parse("jdbc:postgresql://localhost/neo"))
    }

    @Test
    fun `reads the first configured username and password`() {
        val document = OrderedPropertiesCodec.parse(
            "usernameNovanet=neo-user\npasswordNovanet=neo-password\nusernameSpring=ignored\npasswordSpring=ignored\n",
        )
        val keys = DatabaseKeys(
            url = "url",
            database = "database",
            usernames = listOf("usernameNovanet", "usernameSpring"),
            passwords = listOf("passwordNovanet", "passwordSpring"),
        )

        assertEquals(DatabaseCredentials("neo-user", "neo-password"), configuredCredentials(document, keys))
    }
}

package info.macias.nrjmx.connect

import java.util.*
import javax.management.MBeanServerConnection
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import javax.rmi.ssl.SslRMIClientSocketFactory

interface ConnectionFactory {
    fun get(): MBeanServerConnection
}

class JMXConnectionFactory(
        url: String,
        username: String? = null, password: String? = null,
        keyStore: String? = null, keyStorePassword: String? = null,
        trustStore: String? = null, trustStorePassword: String? = null
) : ConnectionFactory, AutoCloseable {
    private val connectionEnv = HashMap<String, Any>()
    private val connector: JMXConnector
    private val address: JMXServiceURL

    init {
        if ("" != username) {
            connectionEnv[JMXConnector.CREDENTIALS] = arrayOf(username, password)
        }

        if (keyStore != null && trustStore != null) {
            // TODO: can't this be added to the connectionEnv instead to system properties?
            val p = System.getProperties()
            p["javax.net.ssl.keyStore"] = keyStore
            if (keyStorePassword != null) {
                p["javax.net.ssl.keyStorePassword"] = keyStorePassword
            }
            p["javax.net.ssl.trustStore"] = trustStore
            if (trustStorePassword != null) {
                p["javax.net.ssl.trustStorePassword"] = trustStorePassword
            }
            connectionEnv["com.sun.jndi.rmi.factory.socket"] = SslRMIClientSocketFactory()
        }

        address = JMXServiceURL(url)
        connector = JMXConnectorFactory.connect(address, connectionEnv)
    }

    override fun get(): MBeanServerConnection {
        return connector.mBeanServerConnection
    }

    override fun close() {
        connector.close()
    }
}
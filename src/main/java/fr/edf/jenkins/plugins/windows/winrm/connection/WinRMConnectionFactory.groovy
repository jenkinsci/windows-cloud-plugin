package fr.edf.jenkins.plugins.windows.winrm.connection

import org.apache.http.client.config.AuthSchemes
import org.kohsuke.accmod.Restricted
import org.kohsuke.accmod.restrictions.NoExternalUse

import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl

import fr.edf.jenkins.plugins.windows.util.CredentialsUtils
import io.cloudsoft.winrm4j.client.WinRmClientContext
import io.cloudsoft.winrm4j.winrm.WinRmTool
import jenkins.model.Jenkins

/**
 * WinRm Connection factory
 * @author CHRIS BAHONDA
 *
 */
class WinRMConnectionFactory {

    /**
     * Dispatch request about the connection method
     * @param config
     * @return WinRmTool
     * @throws WinRMConnectionException
     */
    static WinRmTool getWinRMConnection(WinRMConnectionConfiguration config, WinRmClientContext winRMContext) throws WinRMConnectionException{
        if(config instanceof WinRMGlobalConnectionConfiguration) {
            return getGlobalWinRMConnection(config, winRMContext)
        }
        if(config instanceof WinRMUserConnectionConfiguration) {
            return getUserWinRMConnection(config, winRMContext)
        }
        return null
    }

    /**
     * Generate a global connection for the winrm client
     * @param config
     * @return getConnection
     * @throws WinRMConnectionException
     */

    @Restricted(NoExternalUse)
    private static WinRmTool getGlobalWinRMConnection(WinRMGlobalConnectionConfiguration config = new WinRMGlobalConnectionConfiguration(), WinRmClientContext winRMContext) throws WinRMConnectionException {
        String host = config.host
        Integer port = config.port ?: Integer.valueOf(5985)
        Integer connectionTimeout = config.connectionTimeout ?: Integer.valueOf(1000)
        String authenticationScheme = config.authenticationScheme ?: AuthSchemes.NTLM
        Boolean useHttps = config.useHttps ?: Boolean.FALSE
        def context = config.context ?: Jenkins.get()
        def credentialsId = config.credentialsId ?: null
        if(!credentialsId) {
            throw new WinRMConnectionException("No credentials found for the host " + host)
        }
        def credentials = CredentialsUtils.findCredentials(host, credentialsId, context)
        return getConnection(host, credentials, port, authenticationScheme, useHttps, winRMContext)
    }
    
    /**
     * Generate a connection for the user using their credentials
     * @param config
     * @param winRMContext
     * @return getConnection
     */
    private static WinRmTool getUserWinRMConnection(WinRMUserConnectionConfiguration config = new WinRMUserConnectionConfiguration(), WinRmClientContext winRMContext) {
        String host = config.host
        Integer port = config.port ?: Integer.valueOf(5985)
        Integer connectionTimeout = config.connectionTimeout ?: Integer.valueOf(1000)
        String authenticationScheme = config.authenticationScheme ?: AuthSchemes.NTLM
        Boolean useHttps = config.useHttps ?: Boolean.FALSE
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, 
            "cred", 
            null, 
            config.username, 
            config.password.getPlainText()
            )
            return getConnection(host, credentials, port, authenticationScheme, useHttps, winRMContext)
    }

    /**
     * Creates a WinRm client
     * @param host : hostname of the windows machine
     * @param credentials
     * @param port
     * @param useHttps
     * @return a WinRmTool @see <a href="https://github.com/cloudsoft/winrm4j">https://github.com/cloudsoft/winrm4j</a>
     * @throws WinRMConnectionException
     */

    @Restricted(NoExternalUse)
    private static WinRmTool getConnection(final String host, final StandardCredentials credentials, final Integer port,
            final String authenticationScheme, final Boolean useHttps, WinRmClientContext winRMContext) throws WinRMConnectionException {
        if (credentials instanceof StandardUsernamePasswordCredentials) {
            StandardUsernamePasswordCredentials usernamePasswordCredentials = credentials
            WinRmTool winRmTool = WinRmTool.Builder.builder(host, usernamePasswordCredentials.getUsername(),
                    usernamePasswordCredentials.getPassword().getPlainText())
                    .authenticationScheme(AuthSchemes.NTLM)
                    .port(port.intValue())
                    .useHttps(useHttps.booleanValue())
                    .disableCertificateChecks(true)
                    .context(winRMContext)
                    .build();
            winRmTool.setConnectionTimeout(10000)
            winRmTool.setReceiveTimeout(10000)
            return winRmTool
        } else {
            throw new WinRMConnectionException("Only Username and Password Credentials are allowed")
        }
    }
}

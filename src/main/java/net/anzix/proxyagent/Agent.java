package net.anzix.proxyagent;

import java.lang.instrument.Instrumentation;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.Security;

/**
 * Force to use proxyHost and proxy password parameters.
 *
 * @see: http://rolandtapken.de/blog/2012-04/java-process-httpproxyuser-and-httpproxypassword
 */
public class Agent {
    /**
     * Get the property with proto prefix or if it's missing try without prefix.
     *
     */
    public static String getProperty(String prot, String type) {
        return System.getProperty(prot + "." + type, System.getProperty(type, ""));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        Security.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        // Java ignores http.proxyUser. Here comes the workaround.
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() == RequestorType.PROXY) {
                    String requestingProtocol = getRequestingProtocol();
                    String requestingHost = getRequestingHost();
                    if (requestingProtocol == null || requestingProtocol.isEmpty() ||
                            requestingHost == null || requestingHost.isEmpty()) {
                        return null;
                    }
                    String prot = requestingProtocol.toLowerCase();
                    String host = getProperty(prot, "proxyHost");
                    String port = getProperty(prot, "proxyPort");
                    String user = getProperty(prot, "proxyUser");
                    String password = getProperty(prot, "proxyPassword");
                    if (host == null || host.isEmpty() || port == null || port.isEmpty() ||
                            user == null || user.isEmpty() || password == null || password.isEmpty()) {
                        return null;
                    }
                    int proxyPort;
                    try {
                        proxyPort = Integer.parseInt(port);
                    } catch (NumberFormatException e) {
                        return null;
                    }

                    if (requestingHost.toLowerCase().equals(host.toLowerCase()) &&
                            proxyPort == getRequestingPort()) {
                        // Seems to be OK.
                        return new PasswordAuthentication(user, password.toCharArray());
                    }
                }
                return null;
            }
        });
    }
}

package org.mitre.taxii.client.example;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.xml.bind.JAXBException;
import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mitre.taxii.client.HttpClient;
import org.mitre.taxii.messages.TaxiiXml;
import org.mitre.taxii.messages.xml11.ObjectFactory;
import org.mitre.taxii.messages.xml11.PythonTextOutput;
import org.mitre.taxii.messages.xml11.TaxiiXmlFactory;

/**
 * Provides a set of common command line handling methods and other things
 * common across all the example client apps.
 * 
 * @author jasenj1
 */
abstract class AbstractClient {
    final Cli cli;    
    ObjectFactory factory = new ObjectFactory();
    TaxiiXmlFactory txf = new TaxiiXmlFactory();
    TaxiiXml taxiiXml;
    HttpClient taxiiClient;
    String defaultURL = "http://taxiitest.mitre.org/services/"; // If the "u" command line parameter is not provided, use this value.
          
    AbstractClient() {
        this.cli = new Cli();
        taxiiXml = txf.createTaxiiXml();
    }
    
    public Cli getCli() {
        return cli;
    }
    
    /**
     * Generate a properly configured HttpClient based on the options in the 
     * command line.
     * (The class holds an instance of an object holding the command line, but 
     * I want it to be clear where the configuration information is coming from
     * so it gets passed in.)
     * 
     * @param cmd
     * @return 
     */
    HttpClient generateClient(CommandLine cmd) throws MalformedURLException, Exception {
        HttpClient client = new HttpClient(); // Start with a default TAXII HTTP client.
        
        // Create an Apache HttpClientBuilder to be customized by the command line arguments.
        HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
        
        // Proxy
        if (cmd.hasOption("proxy")) {
            try {
            URL proxyURL = new URL(cmd.getOptionValue("proxy"));
            HttpHost proxyHost = new HttpHost(proxyURL.getHost(), proxyURL.getPort(), proxyURL.getProtocol());
            builder.setProxy(proxyHost);
            } catch (MalformedURLException ex) {
                throw new MalformedURLException("Malformed proxy URL");
            }
        }
        
        // Basic authentication. User & Password
        if (cmd.hasOption("username") ^ cmd.hasOption("password")) {
            throw new Exception("'username' and 'password' arguments are required to appear together.");
        }
        
        if (cmd.hasOption("username") && cmd.hasOption("password")) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(cmd.getOptionValue("username"), cmd.getOptionValue("password")));        
            builder.setDefaultCredentialsProvider(credsProvider);                    
        }

/*   
// TODO: Certificates and Key files is a rather rich and complicated subject. 
// The python library supports a very specific implementation. Eventually support whatever the Python library supports.      
        
        // Certificate and Key authentication.
        if (cmd.hasOption("cert") ^ cmd.hasOption("key")) {
            throw new Exception("'cert' and 'key' arguments are required to appear together.");
        }
        
        if (cmd.hasOption("cert") && cmd.hasOption("key")) {
        }
*/        
                        
        client.setHttpclient(builder.build());
        return client;
    }    
    
    Object doCall(CommandLine cmd, Object request) throws JAXBException, IOException, URISyntaxException{
        
            System.out.println("Request:");
            if (cmd.hasOption("xmloutput")) {
                System.out.println(taxiiXml.marshalToString(request, true));
            } else {
                System.out.println(PythonTextOutput.toText(request));
            }
        
        // Call the service
        Object responseObj = taxiiClient.callTaxiiService(new URI(cmd.getOptionValue("u", defaultURL)), request);

        System.out.println("Response:");        
        if (cmd.hasOption("xmloutput")) {
            System.out.println(taxiiXml.marshalToString(responseObj, true));
        } else {
            System.out.println(PythonTextOutput.toText(responseObj));
        }
        
        return responseObj;
    }
}

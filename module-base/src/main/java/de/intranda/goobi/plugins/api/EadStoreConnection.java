package de.intranda.goobi.plugins.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.jayway.jsonpath.InvalidJsonException;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EadStoreConnection {

    private EadStoreConnection() {
        // hide implicit constructor
    }

    public static boolean checkIfDBIsRunning(String url) {
        if (StringUtils.isBlank(executeRequest(HttpMethod.GET, url))) {
            Helper.setFehlerMeldung("plugin_administration_archive_databaseCannotBeLoaded");
            return false;
        }
        return true;
    }

    public static String executeRequest(HttpMethod method, String url) {

        HttpRequestBase httpBase;

        switch (method) {
            case GET:
                httpBase = new HttpGet(url);
                break;
            case DELETE:
                httpBase = new HttpDelete(url);
                break;
            case PUT:
                httpBase = new HttpPut(url);
                break;
            case POST:
                httpBase = new HttpPost(url);
                break;
            case PATCH:
                httpBase = new HttpPatch(url);
                break;
            default:
                httpBase = new HttpGet(url);
                break;
        }

        CloseableHttpClient client = null;

        try {
            client = HttpClients.createDefault();
            httpBase.setHeader("Accept", "application/xml");
            httpBase.setHeader("Content-type", "application/xml");

            // option for basic auth
            //        CredentialsProvider credsProvider = new BasicCredentialsProvider();
            //        credsProvider.setCredentials(new AuthScope("host", 8080),
            //                new UsernamePasswordCredentials("username", "passowrd"));
            //        client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
            // option to add additional header parameter for token authentication
            //            for (Entry<String, String> entry : headerParameters.entrySet()) {
            //                httpGet.setHeader(entry.getKey(), entry.getValue());
            //            }

            // configue proxy
            setupProxy(url, httpBase);

            return client.execute(httpBase, responseHandler);

        } catch (IOException e) {
            String message = "IOException caught while executing request: " + httpBase.getRequestLine();
            log.error(message);
        } catch (InvalidJsonException e) {
            String message = "ParseException caught while executing request: " + httpBase.getRequestLine();
            log.error(message);
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                log.error(e);
            }
        }

        return null;
    }

    // create a custom response handler
    private static final ResponseHandler<String> responseHandler = response -> {
        HttpEntity entity = response.getEntity();
        return entity != null ? EntityUtils.toString(entity) : null;
    };

    private static void setupProxy(String url, HttpRequestBase method) {
        if (ConfigurationHelper.getInstance().isUseProxy()) {
            try {
                URL ipAsURL = new URL(url);
                if (!ConfigurationHelper.getInstance().isProxyWhitelisted(ipAsURL)) {
                    HttpHost proxy = new HttpHost(ConfigurationHelper.getInstance().getProxyUrl(), ConfigurationHelper.getInstance().getProxyPort());
                    log.debug("Using proxy " + proxy.getHostName() + ":" + proxy.getPort());

                    Builder builder = RequestConfig.custom();
                    builder.setProxy(proxy);

                    RequestConfig rc = builder.build();

                    method.setConfig(rc);
                } else {
                    log.debug("url was on proxy whitelist, no proxy used: " + url);
                }
            } catch (MalformedURLException e) {
                log.debug("could not convert into URL: ", url);
            }

        }
    }

    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH;
    }
}

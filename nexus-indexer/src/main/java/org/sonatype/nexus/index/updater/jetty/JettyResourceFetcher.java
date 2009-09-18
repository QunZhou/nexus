package org.sonatype.nexus.index.updater.jetty;

import static org.codehaus.plexus.util.IOUtil.close;

import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.security.ProxyAuthorization;
import org.eclipse.jetty.client.security.Realm;
import org.eclipse.jetty.client.security.RealmResolver;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethods;
import org.sonatype.nexus.index.updater.ResourceFetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class JettyResourceFetcher
    implements ResourceFetcher
{

    // configuration fields.
    private int maxConnections;

    private int connectionTimeout;

    private boolean useCache;

    private ProxyInfo proxyInfo;

    private AuthenticationInfo authenticationInfo;

    private HttpFields headers;

    // END: configuration fields.

    // transient fields.
    private HttpClient httpClient;

    private String host;

    private String url;

    private final TransferListenerSupport listenerSupport = new TransferListenerSupport();

    public void retrieve( final String name, final File targetFile )
        throws IOException, FileNotFoundException
    {
        HttpFields exchangeHeaders = buildHeaders();
        ResourceExchange exchange = new ResourceExchange( exchangeHeaders );
        exchange.setURL( url );
        exchange.setMethod( HttpMethods.GET );

        httpClient.send( exchange );
        try
        {
            exchange.waitForDone();
        }
        catch ( InterruptedException e )
        {
            IOException err = new IOException( "Transfer interrupted: " + e.getMessage() );
            err.initCause( e );

            throw err;
        }

        int responseStatus = exchange.getResponseStatus();
        switch ( responseStatus )
        {
            case ServerResponse.SC_OK:
            case ServerResponse.SC_NOT_MODIFIED:
                break;

            case ServerResponse.SC_FORBIDDEN:
                throw new IOException( "Transfer failed: [" + responseStatus + "] " + url );

            case ServerResponse.SC_UNAUTHORIZED:
                throw new IOException( "Transfer failed: Not authorized" );

            case ServerResponse.SC_PROXY_AUTHENTICATION_REQUIRED:
                throw new IOException( "Transfer failed: Not authorized by proxy" );

            case ServerResponse.SC_NOT_FOUND:
                throw new IOException( "Transfer failed: " + url + " does not exist" );

            default:
            {
                IOException ex = new IOException( "Transfer failed: [" + responseStatus + "] " + url );
                listenerSupport.fireTransferError( url, ex, TransferEvent.REQUEST_GET );

                throw ex;
            }
        }

        getTransfer( targetFile, exchange );
        targetFile.setLastModified( exchange.getLastModified() );
        listenerSupport.fireGetCompleted( url, targetFile );
    }

    public void connect( final String id, final String url )
        throws IOException
    {
        this.url = url;
        URL u = new URL( url );
        host = u.getHost();

        httpClient = new HttpClient();

        httpClient.setConnectorType( HttpClient.CONNECTOR_SELECT_CHANNEL );
        if ( maxConnections > 0 )
        {
            httpClient.setMaxConnectionsPerAddress( maxConnections );
        }
        if ( connectionTimeout > 0 )
        {
            httpClient.setTimeout( connectionTimeout );
        }

        httpClient.registerListener( NtlmListener.class.getName() );

        NtlmListener.setHelper( new NtlmConnectionHelper( this ) );

        setupClient();

        try
        {
            httpClient.start();
        }
        catch ( Exception e )
        {
            try
            {
                disconnect();
            }
            catch ( IOException internalError )
            {
                // best attempt to make things right.
            }
            finally
            {
                httpClient = null;
            }

            if ( e instanceof IOException )
            {
                throw (IOException) e;
            }

            IOException err = new IOException( e.getLocalizedMessage() );
            err.initCause( e );

            throw err;
        }
    }

    public void disconnect()
        throws IOException
    {
        if ( httpClient != null )
        {
            try
            {
                httpClient.stop();
            }
            catch ( Exception e )
            {
                if ( e instanceof IOException )
                {
                    throw (IOException) e;
                }

                IOException err = new IOException( e.getLocalizedMessage() );
                err.initCause( e );

                throw err;
            }
            finally
            {
                httpClient = null;
            }
        }
    }

    public boolean isUseCache()
    {
        return useCache;
    }

    public ProxyInfo getProxyInfo()
    {
        return proxyInfo;
    }

    public AuthenticationInfo getAuthenticationInfo()
    {
        return authenticationInfo;
    }

    public HttpFields getHttpHeaders()
    {
        return headers;
    }

    protected void setupClient()
        throws IOException
    {
        if ( proxyInfo != null && proxyInfo.getHost() != null )
        {
            String proxyType = proxyInfo.getType();
            if ( !proxyType.equalsIgnoreCase( ProxyInfo.PROXY_HTTP.toLowerCase() ) )
            {
                throw new IOException( "Connection failed: " + proxyType + " is not supported" );
            }

            httpClient.setProxy( new Address( proxyInfo.getHost(), proxyInfo.getPort() ) );

            if ( proxyInfo.getUserName() != null )
            {
                httpClient.setProxyAuthentication( new ProxyAuthorization( proxyInfo.getUserName(),
                                                                           proxyInfo.getPassword() ) );
            }
        }

        final String targetHost = host;

        AuthenticationInfo authInfo = getAuthenticationInfo();
        if ( authInfo != null && authInfo.getUserName() != null )
        {
            httpClient.setRealmResolver( new RealmResolver()
            {
                public Realm getRealm( final String realmName, final HttpDestination destination, final String path )
                    throws IOException
                {
                    return new Realm()
                    {
                        public String getCredentials()
                        {
                            return getAuthenticationInfo().getPassword();
                        }

                        public String getPrincipal()
                        {
                            return getAuthenticationInfo().getUserName();
                        }

                        public String getId()
                        {
                            return targetHost;
                        }
                    };
                }
            } );
        }
    }

    public int getMaxConnections()
    {
        return maxConnections;
    }

    public JettyResourceFetcher setMaxConnections( final int maxConnections )
    {
        this.maxConnections = maxConnections;
        return this;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public JettyResourceFetcher setConnectionTimeout( final int connectionTimeout )
    {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public HttpFields getHeaders()
    {
        return headers;
    }

    public JettyResourceFetcher setHeaders( final HttpFields headers )
    {
        this.headers = headers;
        return this;
    }

    public JettyResourceFetcher setUseCache( final boolean useCache )
    {
        this.useCache = useCache;
        return this;
    }

    public JettyResourceFetcher setProxyInfo( final ProxyInfo proxyInfo )
    {
        this.proxyInfo = proxyInfo;
        return this;
    }

    public JettyResourceFetcher setAuthenticationInfo( final AuthenticationInfo authenticationInfo )
    {
        this.authenticationInfo = authenticationInfo;
        return this;
    }

    public JettyResourceFetcher addTransferListener( final TransferListener listener )
    {
        listenerSupport.addTransferListener( listener );
        return this;
    }

    private void getTransfer( final File targetFile, final ResourceExchange exchange )
        throws IOException
    {
        listenerSupport.fireGetStarted( url, targetFile );

        File destinationDirectory = targetFile.getParentFile();
        if ( destinationDirectory != null && !destinationDirectory.exists() )
        {
            destinationDirectory.mkdirs();
            if ( !destinationDirectory.exists() )
            {
                throw new IOException( "Specified destination directory cannot be created: " + destinationDirectory );
            }
        }

        InputStream input = null;
        OutputStream output = null;
        try
        {
            input = getResponseContentSource( exchange );
            output = new LazyFileOutputStream( targetFile );

            byte[] buffer = new byte[4096];

            TransferEvent transferEvent =
                new TransferEvent( null, listenerSupport.resourceFor( url ), TransferEvent.TRANSFER_PROGRESS,
                                   TransferEvent.REQUEST_GET );

            transferEvent.setTimestamp( System.currentTimeMillis() );

            int remaining = exchange.getContentLength();
            while ( remaining > 0 )
            {
                int n = input.read( buffer, 0, Math.min( buffer.length, remaining ) );

                if ( n == -1 )
                {
                    break;
                }

                listenerSupport.fireTransferProgress( transferEvent, buffer, n );

                output.write( buffer, 0, n );

                remaining -= n;
            }
            output.flush();
        }
        finally
        {
            close( input );
            close( output );
        }
    }

    private InputStream getResponseContentSource( final ResourceExchange exchange )
        throws IOException
    {
        InputStream source = exchange.getResponseContentSource();

        if ( source != null )
        {
            String contentEncoding = exchange.getContentEncoding();
            if ( contentEncoding != null && "gzip".equalsIgnoreCase( contentEncoding ) )
            {
                source = new GZIPInputStream( source );
            }
        }

        return source;
    }

    private HttpFields buildHeaders()
    {
        HttpFields result = new HttpFields();
        if ( headers != null )
        {
            result.add( headers );
        }
        else
        {
            result.add( "Accept-Encoding", "gzip" );
            if ( !useCache )
            {
                result.add( "Pragma", "no-cache" );
                result.add( "Cache-Control", "no-cache, no-store" );
            }
        }

        return result;
    }

}

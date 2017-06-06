/*******************************************************************************
 * Copyright (c) 2017 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.neoscada.contrib.status;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class StatusEndpoint
{
    private final static Logger logger = LoggerFactory.getLogger ( StatusEndpoint.class );

    private static final Charset UTF8 = Charset.forName ( "UTF-8" );

    private final Map<String, ServerGroup> cfg;

    private final int port;

    public StatusEndpoint ( Map<String, ServerGroup> cfg, int port )
    {
        this.cfg = cfg;
        this.port = port;
    }

    public void run ()
    {
        for ( Entry<String, ServerGroup> entry : cfg.entrySet () )
        {
            entry.getValue ().initialize ();
        }
        startHttpServer ();
    }

    private void startHttpServer ()
    {
        Undertow server = Undertow.builder ().addHttpListener ( port, "0.0.0.0" ).setHandler ( new HttpHandler () {
            @Override
            public void handleRequest ( final HttpServerExchange exchange ) throws Exception
            {
                exchange.getResponseHeaders ().put ( Headers.CONTENT_TYPE, "text/plain" );
                String result = createStatusMap ( exchange.getRelativePath () );
                if ( result == null )
                {
                    exchange.setStatusCode ( 404 );
                }
                else
                {
                    if ( result.startsWith ( "CRIT" ) )
                    {
                        exchange.setStatusCode ( 500 );
                    }
                    exchange.getResponseSender ().send ( result, UTF8 );
                }
            }
        } ).build ();
        logger.info ( "starting server ..." );
        server.start ();
    }

    protected String createStatusMap ( String path )
    {
        String[] parts = path.split ( "/" );
        if ( parts.length >= 3 )
        {
            ServerGroup serverGroup = cfg.get ( parts[1] );
            if ( serverGroup == null )
            {
                return null;
            }
            if ( "status".equals ( parts[2] ) )
            {
                return serverGroup.renderStatus ();
            }
            else
            {
                return serverGroup.renderHealth ();
            }
        }
        return null;
    }
}

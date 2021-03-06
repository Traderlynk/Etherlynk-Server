/**
 * $Revision $
 * $Date $
 *
 * Copyright (C) 2005-2010 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.plugin.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A filter that forwards URLs to the jitsi-meet welcome page.
 *
 * The URLs that are forwarded are those that reference jitsi-rooms. These are identified as URLs that start with the
 * context on which the jitsi-meet application is hosted (as part of the OFMeet plugin), but excludes any further child
 * context.
 *
 * For example, these paths are forwarded:
 * <ul>
 *     <li>/meet/myroom</li>
 *     <li>/meet/my.room</li>
 * </ul>
 *
 * These paths are <em>not</em> forwarded:
 * <ul>
 *     <li>/meet/room.js</li>
 *     <li>/meet/room.png</li>
 *     <li>/meet/childcontext/anything</li>
 * </ul>
 *
 * on any " followed by an exclusion of
 * patterns that match well-known file extensions (eg: css, javascript, images)
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class JitsiMeetRedirectFilter implements Filter
{
    private static final Logger Log = LoggerFactory.getLogger( JitsiMeetRedirectFilter.class );

    private final Set<String> excludedExtensions = new HashSet<>();

    @Override
    public void init( FilterConfig filterConfig ) throws ServletException
    {
        excludedExtensions.clear();
        excludedExtensions.add( "png" );
        excludedExtensions.add( "gif" );
        excludedExtensions.add( "jpg" );
        excludedExtensions.add( "ico" );
        excludedExtensions.add( "css" );
        excludedExtensions.add( "js" );
        excludedExtensions.add( "jsp" );
        excludedExtensions.add( "json");
    }

    protected boolean hasCorrectContext( HttpServletRequest request )
    {
        return request.getRequestURI().matches( request.getContextPath() + "/([^/]+)$" );
    }

    protected boolean containsExcludedExtension( HttpServletRequest request )
    {
        final String uri = request.getRequestURI().toLowerCase();
        for ( final String excludedExtension : excludedExtensions )
        {
            if ( uri.contains("/custom") || uri.contains("/proxy") || uri.contains("/config") || uri.endsWith( "." + excludedExtension ) || uri.contains( "." + excludedExtension + "?" ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse response, FilterChain filterChain ) throws IOException, ServletException
    {
        if ( servletRequest instanceof HttpServletRequest )
        {
            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            if ( !hasCorrectContext( request ) )
            {
                Log.trace( "Not forwarding " + request.getRequestURI() + " (does not have correct context)." );
            }
            else if ( containsExcludedExtension( request ) )
            {
                Log.trace( "Not forwarding " + request.getRequestURI() + " (contains excluded extension)." );
            }
            else
            {
                Log.trace( "Forwarding " + request.getRequestURI() + " to /" );
                RequestDispatcher dispatcher = request.getRequestDispatcher( "/" );
                dispatcher.forward( request, response );
                return;
            }
        }
        filterChain.doFilter( servletRequest, response );
    }

    @Override
    public void destroy()
    {
    }
}

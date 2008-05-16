/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.rest.schedules;

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Scm;
import org.restlet.data.MediaType;
import org.sonatype.nexus.rest.model.AuthenticationClientPermissions;
import org.sonatype.nexus.rest.model.AuthenticationLoginResource;
import org.sonatype.nexus.rest.model.AuthenticationLoginResourceResponse;
import org.sonatype.nexus.rest.model.AuthenticationSettings;
import org.sonatype.nexus.rest.model.ConfigurationsListResource;
import org.sonatype.nexus.rest.model.ConfigurationsListResourceResponse;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.FeedListResource;
import org.sonatype.nexus.rest.model.FeedListResourceResponse;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResourceResponse;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResourceResponse;
import org.sonatype.nexus.rest.model.LogsListResource;
import org.sonatype.nexus.rest.model.LogsListResourceResponse;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.NexusError;
import org.sonatype.nexus.rest.model.NexusErrorResponse;
import org.sonatype.nexus.rest.model.NexusResponse;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettings;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryMetaResource;
import org.sonatype.nexus.rest.model.RepositoryMetaResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryRouteListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.rest.model.RepositoryRouteResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.rest.model.RepositoryStatusListResource;
import org.sonatype.nexus.rest.model.RepositoryStatusListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceAdvancedResource;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceDailyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceMonthlyResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.nexus.rest.model.SearchResponse;
import org.sonatype.nexus.rest.model.StatusConfigurationValidationResponse;
import org.sonatype.nexus.rest.model.StatusResource;
import org.sonatype.nexus.rest.model.StatusResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.plexus.rest.xstream.json.JsonOrgHierarchicalStreamDriver;
import org.sonatype.plexus.rest.xstream.json.PrimitiveKeyedMapConverter;

import com.thoughtworks.xstream.XStream;

public class ScheduledServiceResponseTest
    extends TestCase
{

    protected XStream xstream;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // create and configure XStream for JSON
        xstream = new XStream( new JsonOrgHierarchicalStreamDriver() );

        // aliasaes
        // NexusResponse
        xstream.alias( "artifact", NexusArtifact.class );

        // Maven POM
        xstream.alias( "project", Model.class );

        // omitting modelEncoding
        xstream.omitField( NexusErrorResponse.class, "modelEncoding" );
        xstream.omitField( NexusError.class, "modelEncoding" );

        xstream.omitField( ContentListResourceResponse.class, "modelEncoding" );
        xstream.omitField( ContentListResource.class, "modelEncoding" );

        xstream.omitField( RepositoryResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryBaseResource.class, "modelEncoding" );
        xstream.omitField( RepositoryResource.class, "modelEncoding" );
        xstream.omitField( RepositoryProxyResource.class, "modelEncoding" );
        xstream.omitField( RepositoryShadowResource.class, "modelEncoding" );
        xstream.omitField( RepositoryResourceRemoteStorage.class, "modelEncoding" );

        xstream.omitField( RepositoryListResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryListResource.class, "modelEncoding" );

        xstream.omitField( RepositoryStatusResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryStatusResource.class, "modelEncoding" );

        xstream.omitField( RepositoryStatusListResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryStatusListResource.class, "modelEncoding" );

        xstream.omitField( RepositoryMetaResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryMetaResource.class, "modelEncoding" );

        xstream.omitField( RepositoryGroupListResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryGroupListResource.class, "modelEncoding" );

        xstream.omitField( RepositoryGroupResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryGroupResource.class, "modelEncoding" );
        xstream.omitField( RepositoryGroupMemberRepository.class, "modelEncoding" );

        xstream.omitField( RepositoryRouteListResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryRouteListResource.class, "modelEncoding" );

        xstream.omitField( RepositoryRouteResourceResponse.class, "modelEncoding" );
        xstream.omitField( RepositoryRouteResource.class, "modelEncoding" );
        xstream.omitField( RepositoryRouteMemberRepository.class, "modelEncoding" );

        xstream.omitField( GlobalConfigurationListResourceResponse.class, "modelEncoding" );
        xstream.omitField( GlobalConfigurationListResource.class, "modelEncoding" );

        xstream.omitField( GlobalConfigurationResourceResponse.class, "modelEncoding" );
        xstream.omitField( GlobalConfigurationResource.class, "modelEncoding" );
        xstream.omitField( RemoteConnectionSettings.class, "modelEncoding" );
        xstream.omitField( RemoteHttpProxySettings.class, "modelEncoding" );
        xstream.omitField( AuthenticationSettings.class, "modelEncoding" );

        xstream.omitField( LogsListResourceResponse.class, "modelEncoding" );
        xstream.omitField( LogsListResource.class, "modelEncoding" );

        xstream.omitField( ConfigurationsListResourceResponse.class, "modelEncoding" );
        xstream.omitField( ConfigurationsListResource.class, "modelEncoding" );

        xstream.omitField( FeedListResourceResponse.class, "modelEncoding" );
        xstream.omitField( FeedListResource.class, "modelEncoding" );

        xstream.omitField( SearchResponse.class, "modelEncoding" );

        xstream.omitField( NexusResponse.class, "modelEncoding" );
        xstream.omitField( NexusArtifact.class, "modelEncoding" );

        xstream.omitField( AuthenticationLoginResourceResponse.class, "modelEncoding" );
        xstream.omitField( AuthenticationLoginResource.class, "modelEncoding" );
        xstream.omitField( AuthenticationClientPermissions.class, "modelEncoding" );

        xstream.omitField( StatusResource.class, "modelEncoding" );
        xstream.omitField( StatusResourceResponse.class, "modelEncoding" );
        xstream.omitField( StatusConfigurationValidationResponse.class, "modelEncoding" );
        
        xstream.omitField( ScheduledServiceListResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceListResourceResponse.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceBaseResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServicePropertyResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceDailyResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceAdvancedResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceMonthlyResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceWeeklyResource.class, "modelEncoding" );
        xstream.omitField( ScheduledServiceResourceResponse.class, "modelEncoding" );

        // Maven model
        xstream.omitField( Model.class, "modelEncoding" );
        xstream.omitField( ModelBase.class, "modelEncoding" );
        xstream.omitField( Scm.class, "modelEncoding" );
        // for JSON, we use a custom converter for Maps
        xstream.registerConverter( new PrimitiveKeyedMapConverter( xstream.getMapper() ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    public void testNoScheduledService()
        throws Exception
    {
        String jsonString = "{ \"org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse\" : " +
            "{\"data\":{\"id\":null,\"name\":\"test\",\"serviceType\":\"Synchronize Repositories\",\"serviceSchedule\":\"none\",\"enabled\":false}}}";
        XStreamRepresentation representation = new XStreamRepresentation(
            xstream,
            jsonString,
            MediaType.APPLICATION_JSON );

        ScheduledServiceListResourceResponse response = (ScheduledServiceListResourceResponse) representation
            .getPayload( new ScheduledServiceListResourceResponse() );

    }
    public void testOnceScheduledService()
        throws Exception
    {
        String jsonString = "{ \"org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse\" : " +
            "{\"data\":{\"id\":null,\"name\":\"test\",\"serviceType\":\"Synchronize Repositories\",\"serviceSchedule\":\"once\",\"enabled\":false,\"startDate\":\"1210651200000\",\"startTime\":\"12:30\"}}}";
        XStreamRepresentation representation = new XStreamRepresentation(
            xstream,
            jsonString,
            MediaType.APPLICATION_JSON );
    
        ScheduledServiceListResourceResponse response = (ScheduledServiceListResourceResponse) representation
            .getPayload( new ScheduledServiceListResourceResponse() );
    
    }
    public void testDailyScheduledService()
        throws Exception
    {
        String jsonString = "{ \"org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse\" : " +
            "{\"data\":{\"id\":null,\"name\":\"test\",\"serviceType\":\"Synchronize Repositories\",\"serviceSchedule\":\"daily\",\"enabled\":false,\"startDate\":\"1210651200000\",\"startTime\":\"12:30\",\"recurringTime\":\"12:30\"}}}";
        XStreamRepresentation representation = new XStreamRepresentation(
            xstream,
            jsonString,
            MediaType.APPLICATION_JSON );
    
        ScheduledServiceListResourceResponse response = (ScheduledServiceListResourceResponse) representation
            .getPayload( new ScheduledServiceListResourceResponse() );
    
    }
    /*public void testWeeklyScheduledService()
        throws Exception
    {
        String jsonString = "{ \"org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse\" : " +
            "{\"data\":{\"id\":null,\"name\":\"test\",\"serviceType\":\"Synchronize Repositories\",\"serviceSchedule\":\"weekly\",\"enabled\":false,\"startDate\":\"1210651200000\",\"startTime\":\"12:30\",\"recurringTime\":\"12:30\",\"recurringDay\":[\"Monday\",\"Wednesday\"]}}}";
        XStreamRepresentation representation = new XStreamRepresentation(
            xstream,
            jsonString,
            MediaType.APPLICATION_JSON );
    
        ScheduledServiceListResourceResponse response = (ScheduledServiceListResourceResponse) representation
            .getPayload( new ScheduledServiceListResourceResponse() );
    
    }
    public void testMonthlyScheduledService()
        throws Exception
    {
        String jsonString = "{ \"org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse\" : " +
            "{\"data\":{\"id\":null,\"name\":\"test\",\"serviceType\":\"Synchronize Repositories\",\"serviceSchedule\":\"monthly\",\"enabled\":false,\"startDate\":\"1210651200000\",\"startTime\":\"12:30\",\"recurringTime\":\"12:30\",\"recurringDay\":[\"1\",\"2\"]}}}";
        XStreamRepresentation representation = new XStreamRepresentation(
            xstream,
            jsonString,
            MediaType.APPLICATION_JSON );
    
        ScheduledServiceListResourceResponse response = (ScheduledServiceListResourceResponse) representation
            .getPayload( new ScheduledServiceListResourceResponse() );
    
    }*/
    public void testAdvancedScheduledService()
        throws Exception
    {
        String jsonString = "{ \"org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse\" : " +
            "{\"data\":{\"id\":null,\"name\":\"test\",\"serviceType\":\"Synchronize Repositories\",\"serviceSchedule\":\"advanced\",\"enabled\":false}}}";
        XStreamRepresentation representation = new XStreamRepresentation(
            xstream,
            jsonString,
            MediaType.APPLICATION_JSON );
    
        ScheduledServiceListResourceResponse response = (ScheduledServiceListResourceResponse) representation
            .getPayload( new ScheduledServiceListResourceResponse() );
    
    }
}

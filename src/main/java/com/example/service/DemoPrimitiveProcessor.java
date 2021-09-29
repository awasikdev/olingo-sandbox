package com.example.service;

import com.example.data.Storage;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.*;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.*;

import java.io.*;
import java.util.*;

public class DemoPrimitiveProcessor implements PrimitiveProcessor {

    private OData odata;
    private Storage storage;
    private ServiceMetadata serviceMetadata;

    public DemoPrimitiveProcessor(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        // 1. Retrieve info from URI
        // 1.1. retrieve the info about the requested entity set
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        // Note: only in our example we can rely that the first segment is the EntitySet
        UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
        EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();
        // the key for the entity
        List<UriParameter> keyPredicates = uriEntityset.getKeyPredicates();

        // 1.2. retrieve the requested (Edm) property
        // the last segment is the Property
        UriResourceProperty uriProperty = (UriResourceProperty) resourceParts.get(resourceParts.size() -1);
        EdmProperty edmProperty = uriProperty.getProperty();
        String edmPropertyName = edmProperty.getName();
        // in our example, we know we have only primitive types in our model
        EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();

        // 2. retrieve data from backend
        // 2.1. retrieve the entity data, for which the property has to be read
        Entity entity = storage.readEntityData(edmEntitySet, keyPredicates);
        if (entity == null) { // Bad request
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // 2.2. retrieve the property data from the entity
        Property property = entity.getProperty(edmPropertyName);
        if (property == null) {
            throw new ODataApplicationException("Property not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // 3. serialize
        Object value = property.getValue();
        if (value != null) {
            // 3.1. configure the serializer
            ODataSerializer serializer = odata.createSerializer(responseFormat);

            ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).navOrPropertyPath(edmPropertyName).build();
            PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
            // 3.2. serialize
            SerializerResult serializerResult = serializer.primitive(serviceMetadata, edmPropertyType, property, options);
            InputStream propertyStream = serializerResult.getContent();

            //4. configure the response object
            response.setContent(propertyStream);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        }else{
            // in case there's no value for the property, we can skip the serialization
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        }
    }

    @Override
    public void updatePrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType, ContentType contentType1) throws ODataApplicationException, ODataLibraryException {

    }

    @Override
    public void deletePrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {

    }
}

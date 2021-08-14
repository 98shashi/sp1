package uk.co.compendiumdev.challenge.challengesrouting;

import uk.co.compendiumdev.thingifier.Thingifier;
import uk.co.compendiumdev.thingifier.api.ThingifierApiDefn;
import uk.co.compendiumdev.thingifier.api.http.HttpApiRequest;
import uk.co.compendiumdev.thingifier.api.http.ThingifierHttpApi;
import uk.co.compendiumdev.thingifier.api.response.ApiResponse;
import uk.co.compendiumdev.thingifier.application.routehandlers.HttpApiRequestHandler;
import uk.co.compendiumdev.thingifier.application.routehandlers.SparkApiRequestResponseHandler;
import uk.co.compendiumdev.thingifier.core.Thing;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.Field;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.FieldType;
import uk.co.compendiumdev.thingifier.core.domain.definitions.validation.MaximumLengthValidationRule;
import uk.co.compendiumdev.thingifier.core.domain.instances.ThingInstance;
import uk.co.compendiumdev.thingifier.reporting.JsonThing;
import uk.co.compendiumdev.thingifier.spark.SimpleRouteConfig;

import java.util.ArrayList;
import java.util.List;

import static spark.Spark.*;

public class SimulationRoutes {

    private ThingifierHttpApi httpApi;
    private JsonThing jsonThing;
    public Thingifier simulation;
    public Thing entityDefn;

    public void setUpData(){
        // fake the data storage
        this.simulation = new Thingifier();

        this.simulation.apiConfig().setResponsesToShowGuids(false);

        this.entityDefn = this.simulation.createThing("entity", "entities");

        this.entityDefn.definition().addFields(
                Field.is("id", FieldType.ID),
                Field.is("name", FieldType.STRING).
                        makeMandatory().
                        withValidation(new MaximumLengthValidationRule(50)).
                        withDefaultValue("unnamed"),
                Field.is("description", FieldType.STRING).
                        withDefaultValue("").
                        withValidation(new MaximumLengthValidationRule(200))
        );

        for(int id=1; id<=10; id++){

            this.entityDefn.createManagedInstance().
                        //setValue("id", String.valueOf(id)).
                        setValue("name", "entity number " +id);
        }

        this.entityDefn.createManagedInstance().
                //setValue("id", String.valueOf(id)).
                        setValue("name", "bob");

        // this gives us access to the common http processing functions
        this.httpApi = new ThingifierHttpApi(this.simulation);
        this.jsonThing = new JsonThing(this.simulation.apiConfig().jsonOutput());
    }

    public void configure(final ThingifierApiDefn apiDefn) {

        setUpData();

        // /sim should be the GUI
        String endpoint = "/sim/entities";

        // redirect a GET to "/fromPath" to "/toPath" for GUI
        redirect.get("/sim", "/simulation.html");

        options(endpoint, (request, result) -> {
            result.status(204);
            result.header("Allow", "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE");
            return "";
        });

        new SimpleRouteConfig(endpoint).
                status(501, "patch", "trace");

        new SimpleRouteConfig(endpoint + "/*").
                status(501, "patch", "trace");

        options(endpoint + "/*", (request, result) -> {
            result.status(204);
            result.header("Allow", "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE");
            return "";
        });

        HttpApiRequestHandler getEntitiesHandler = (HttpApiRequest anHttpApiRequest) ->{
            // remove id 11 because that is a POST so not available in the list
            List<Integer> idsToRemove = new ArrayList<>();
            idsToRemove.add(11);

            // process it because the request validated
            List<ThingInstance> instances = new ArrayList();
            for (ThingInstance possible : this.entityDefn.getInstances()) {
                if (!idsToRemove.contains(
                        possible.getFieldValue("id").asInteger())) {
                    instances.add(possible);
                }
            }

            return ApiResponse.success().returnInstanceCollection(instances);
        };

        get(endpoint, (request, result) -> {
            return new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler(getEntitiesHandler).handle();
        });

        head(endpoint, (request, result) -> {

            new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler(getEntitiesHandler).handle();
            return "";
        });

        HttpApiRequestHandler getEntityHandler = (HttpApiRequest anHttpApiRequest) ->{

            ApiResponse response=null;

            // process it because the request validated
            String id = anHttpApiRequest.getUrlParam(":id");
            ThingInstance instance = this.entityDefn.findInstanceByGUIDorID(id);
            if (instance == null) {
                response = ApiResponse.error404("Could not find Entity with ID " + id);
            } else {
                response = ApiResponse.success().returnSingleInstance(instance);
            }

            if (id.equals("10")) {
                // 10 is the entity we amend to name:eris
                ThingInstance fake = this.entityDefn.createInstance().
                        overrideValue("id", "10").setValue("name", "eris");
                instance = fake;
                response = ApiResponse.success().returnSingleInstance(instance);
            }

            if (id.equals("9")) {
                // 9 is the entity we delete
                response = ApiResponse.error404("Could not find Entity with ID 9");
            }

            return response;
        };

        // get a specific entity
        get(endpoint + "/:id", (request, result) -> {

            return new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler(getEntityHandler)
                    .handle();
        });

        head(endpoint + "/:id", (request, result) -> {

            new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler(getEntityHandler)
                    .handle();

            return "";
        });

        // post create new - will create as 11 {"name":"bob"}
        post(endpoint, (request, result) -> {

            return new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler((anHttpApiRequest) ->{
                        return ApiResponse.created(this.entityDefn.
                                        findInstanceByGUIDorID("11"),
                                this.simulation.apiConfig());
                    }).handle();
        });

        HttpApiRequestHandler putAndPostEntityHandler = (HttpApiRequest anHttpApiRequest) -> {
            // process it because the request validated
            ApiResponse response = null;
            String id = anHttpApiRequest.getUrlParam(":id");
            if (id.equals("11")) {
                // we can create id 11
                response = ApiResponse.created(
                        this.entityDefn.findInstanceByGUIDorID("11"),
                        this.simulation.apiConfig());
            } else {
                if (id.equals("10")) {
                    // 10 is the entity we amend to name:eris
                    ThingInstance fake = this.entityDefn.createInstance().
                            overrideValue("id", "10").setValue("name", "eris");
                    response = ApiResponse.success().returnSingleInstance(fake);
                } else {
                    final ThingInstance instance = this.entityDefn.findInstanceByGUIDorID(id);
                    if (instance == null) {
                        if(anHttpApiRequest.getVerb()== HttpApiRequest.VERB.POST) {
                            response = ApiResponse.error404("Could not find Entity with ID " + id);
                        }else{ // must be a PUT
                            response = ApiResponse.error(403, "Not authorised to create that entity");
                        }
                    } else {
                        response = ApiResponse.error(403, "Not authorised to amend that entity");
                    }
                }
            }
            return response;
        };

        // post amend 10
        // post create - 11
        post(endpoint + "/:id", (request, result) -> {
            return new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler(putAndPostEntityHandler).handle();
        });

        // put specific id will create (11),
        //  and can amend with put (10)
        put(endpoint + "/:id", (request, result) -> {
            return new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler(putAndPostEntityHandler).handle();
        });

        delete(endpoint + "/:id", (request, result) -> {

            return new SparkApiRequestResponseHandler(request, result, simulation).
                    usingHandler((anHttpApiRequest) ->{
                        ApiResponse response = null;
                        String id = anHttpApiRequest.getUrlParam(":id");
                        if (id.equals("9")) {
                            // we can delete id 9
                            response = new ApiResponse(204);
                        } else {
                            final ThingInstance instance = this.entityDefn.findInstanceByGUIDorID(id);
                            if (instance == null) {
                                response = ApiResponse.error404("Could not find Entity with ID " + id);
                            } else {
                                response = ApiResponse.error(403, "Not authorised to delete that entity");
                            }
                        }
                        return response;
                    }).handle();
        });
    }
}
package uk.co.compendiumdev.integration.api_non_http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.co.compendiumdev.thingifier.Thing;
import uk.co.compendiumdev.thingifier.Thingifier;
import uk.co.compendiumdev.thingifier.api.response.ApiResponse;
import uk.co.compendiumdev.thingifier.domain.definitions.fielddefinition.FieldType;
import uk.co.compendiumdev.thingifier.domain.definitions.fielddefinition.Field;
import uk.co.compendiumdev.thingifier.domain.instances.ThingInstance;

import static uk.co.compendiumdev.thingifier.domain.definitions.fielddefinition.FieldType.STRING;

public class IdBasedInstanceNonHttpTest {

    public Thingifier getThingifier() {
        Thingifier thingifier = new Thingifier();
        thingifier.setDocumentation("Model", "test model");

        Thing thing = thingifier.createThing("thing", "things");
        thing.definition()
                .addFields(Field.is("title", STRING),
                        Field.is("id", FieldType.ID)
                )
        ;

        return thingifier;
    }


    @Test
    public void canGetAThingUsingIdOrGuid(){

        Thingifier model = getThingifier();

        final Thing thing = model.getThingNamed("thing");
        final ThingInstance existingInstance = thing.createInstance().setValue("title",
                "My Title" + System.nanoTime());
        thing.addInstance(existingInstance);

        final ApiResponse apiResponse = model.api().get("/thing/" + existingInstance.getGUID());
        Assertions.assertEquals(200, apiResponse.getStatusCode());
        Assertions.assertEquals(existingInstance, apiResponse.getReturnedInstance());

        final ApiResponse idApiResponse = model.api().get("/thing/" + existingInstance.getFieldValue("id").asString());
        Assertions.assertEquals(200, idApiResponse.getStatusCode());
        Assertions.assertEquals(existingInstance, idApiResponse.getReturnedInstance());

    }

}

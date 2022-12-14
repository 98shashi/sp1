package uk.co.compendiumdev.integration.api_non_http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.co.compendiumdev.thingifier.core.domain.definitions.EntityDefinition;
import uk.co.compendiumdev.thingifier.core.domain.instances.EntityInstanceCollection;
import uk.co.compendiumdev.thingifier.Thingifier;
import uk.co.compendiumdev.thingifier.api.response.ApiResponse;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.FieldType;
import uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.Field;
import uk.co.compendiumdev.thingifier.core.domain.instances.EntityInstance;

import static uk.co.compendiumdev.thingifier.core.domain.definitions.field.definition.FieldType.STRING;

public class IdBasedInstanceNonHttpTest {

    public Thingifier getThingifier() {
        Thingifier thingifier = new Thingifier();
        thingifier.setDocumentation("Model", "test model");

        EntityDefinition thing = thingifier.defineThing("thing", "things");
        thing
                .addFields(Field.is("title", STRING),
                        Field.is("id", FieldType.ID)
                )
        ;

        return thingifier;
    }


    @Test
    public void canGetAThingUsingIdOrGuid(){

        Thingifier model = getThingifier();

        final EntityInstanceCollection thing = model.getThingInstancesNamed("thing");
        final EntityInstance existingInstance = thing.createManagedInstance().setValue("title",
                "My Title" + System.nanoTime());

        final ApiResponse apiResponse = model.api().get("/thing/" + existingInstance.getGUID());
        Assertions.assertEquals(200, apiResponse.getStatusCode());
        Assertions.assertEquals(existingInstance, apiResponse.getReturnedInstance());

        final ApiResponse idApiResponse = model.api().get("/thing/" + existingInstance.getFieldValue("id").asString());
        Assertions.assertEquals(200, idApiResponse.getStatusCode());
        Assertions.assertEquals(existingInstance, idApiResponse.getReturnedInstance());

    }

}

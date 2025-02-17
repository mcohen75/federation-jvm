package com.apollographql.federation.graphqljava;

import graphql.ExecutionResult;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.errors.SchemaProblem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FederationTest {
    @Test
    void testEmpty() {
        final String sdl = TestUtils.readResource("empty.graphql");
        final GraphQLSchema schema = SchemaUtils.buildSchema(sdl);

        final GraphQLSchema federated = Federation.transform(schema).build();
        Assertions.assertEquals("type Query {\n" +
                "  _service: _Service\n" +
                "}\n" +
                "\n" +
                "type _Service {\n" +
                "  sdl: String!\n" +
                "}\n", SchemaUtils.printSchema(federated));

        final GraphQLType _Service = federated.getType("_Service");
        assertNotNull(_Service, "_Service type present");
        final GraphQLFieldDefinition _service = federated.getQueryType().getFieldDefinition("_service");
        assertNotNull(_service, "_service field present");
        assertEquals(_Service, _service.getType(), "_service returns _Service");

        SchemaUtils.assertSDL(federated, sdl);
    }

    @Test
    void testRequirements() {
        final GraphQLSchema schema = SchemaUtils.buildSchema(TestUtils.readResource("product.graphql"));

        assertThrows(SchemaProblem.class, () ->
                Federation.transform(schema).build());
        assertThrows(SchemaProblem.class, () ->
                Federation.transform(schema).resolveEntityType(env -> null).build());
        assertThrows(SchemaProblem.class, () ->
                Federation.transform(schema).fetchEntities((DataFetcher) env -> null).build());
    }

    @Test
    void testSimpleService() {
        final String sdl = TestUtils.readResource("product.graphql");
        final GraphQLSchema schema = SchemaUtils.buildSchema(sdl);

        final GraphQLSchema federated = Federation.transform(schema)
                .fetchEntities((DataFetcher) env ->
                        env.<List<Map<String, Object>>>getArgument(_Entity.argumentName)
                                .stream()
                                .map(map -> {
                                    if ("Product".equals(map.get("__typename"))) {
                                        return Product.PLANCK;
                                    }
                                    return null;
                                })
                                .collect(Collectors.toList()))
                .resolveEntityType(env -> env.getSchema().getObjectType("Product"))
                .build();

        SchemaUtils.assertSDL(federated, sdl);

        final ExecutionResult result = SchemaUtils.execute(federated, "{\n" +
                "  _entities(representations: [{__typename:\"Product\"}]) {\n" +
                "    ... on Product { price }\n" +
                "  }" +
                "}");
        assertEquals(0, result.getErrors().size(), "No errors");
        final Map<String, Object> data = result.getData();
        final List<Map<String, Object>> _entities = (List<Map<String, Object>>) data.get("_entities");
        assertEquals(1, _entities.size());
        assertEquals(180, _entities.get(0).get("price"));
    }
}

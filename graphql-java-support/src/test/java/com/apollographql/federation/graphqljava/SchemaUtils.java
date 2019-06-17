package com.apollographql.federation.graphqljava;

import graphql.ExecutionResult;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Map;

import static graphql.ExecutionInput.newExecutionInput;
import static graphql.GraphQL.newGraphQL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class SchemaUtils {
    private static final RuntimeWiring noop = RuntimeWiring.newRuntimeWiring().build();

    static GraphQLSchema buildSchema(String sdl) {
        final TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);

        final RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .scalar(_FieldSet.type)
                .build();

        final SchemaGenerator.Options options = SchemaGenerator.Options
                .defaultOptions()
                .enforceSchemaDirectives(false);

        return new SchemaGenerator().makeExecutableSchema(
                options,
                typeRegistry,
                wiring);
    }

    static String printSchema(GraphQLSchema schema) {
        return new SchemaPrinter().print(schema);
    }

    static ExecutionResult execute(GraphQLSchema schema, String query) {
        return newGraphQL(schema).build().execute(newExecutionInput().query(query).build());
    }

    static void assertSDL(GraphQLSchema schema, String expected) {
        final ExecutionResult inspect = execute(schema, "{_service{sdl}}");
        assertEquals(0, inspect.getErrors().size(), "No errors");
        final Map<String, Object> data = inspect.getData();
        assertNotNull(data);
        final Map<String, Object> _service = (Map<String, Object>) data.get("_service");
        assertNotNull(_service);
        final String sdl = (String) _service.get("sdl");
        assertEquals(expected.trim(), sdl.trim());
    }

    private SchemaUtils() {
    }
}

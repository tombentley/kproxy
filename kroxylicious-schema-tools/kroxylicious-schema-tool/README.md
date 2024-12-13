A "compiler" for JSON Schema (specifically Wright-00, aka Draft 5), which generates `.java` 
source code with Jackson annotations that allows deserialization of a schema-valid JSON instance into a tree of POJOs.

# Non-validation

The Java classes resulting from compilation assume that the JSON instance is  schema-valid. No validation is done by the generated classes so it is the user's responsibility to ensure this assumption is met. If in doubt, validate prior to deserialization. Likewise, it is perfectly possible to use the Java API to construct a POJO tree that, upon serialization, is not schema valid.

# No compatibility for schema evoluation

In general, backwards compatible evolution of the JSON schema will not result in backwards (binary) compatible evolution of the POJO API. For example, adding a non-`required` property to an `object`-typed schema will result in a change in constructor signature (to add a parameter for the new property), meaning existing call-sites will be broken in both source and binary.

## Type Mapping

| JSON Schema                                      | Java                                                                                                                                                 |
|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type: boolean`                                  | `java.lang.Boolean` (not primitive `boolean`)                                                                                                        |
| `type: integer`                                  | `java.lang.Long` (not primitive `long`)                                                                                                              |
| `type: number`                                   | `java.lang.Double` (not primitive `double`)                                                                                                          |
| `type: string`                                   | `java.lang.String`                                                                                                                                   |
| `type: string` with `format: url`                | `java.net.URI`                                                                                                                                       |
| `type: object` with `properties`                 | Java class with fields, accessor and mutator methods for each property.                                                                              |
| `type: object` with `additionalProperties: true` | Add `@JsonAnyGetter` and `@JsonAnySetter`                                                                                                            |
| `type: array`                                    | `java.util.List<T> `, where `T` is the type of the schema `items`                                                                                    |
| `type: array` with `x-kubernetes-list-type: set` | `java.util.Set<T> `, where `T` is the type of the schema `items`                                                                                     |
| `type: array` with `x-kubernetes-list-type: map` | `java.util.Map<K, T> `, where `T` is the type of the schema `items`, and `K` is a POJO based on the properties given in `x-kubernetes-list-map-keys` |

Other JSON Schema keywords, such as `maximum`, `minimum`, `pattern`, `minProperties` or `maxItems` etc., are ignored.

When a Java class is generated the (unqualified) class name may be given explicitly via the `$javaType` schema keyword. Otherwise, a name is constructed based on the filename of the schema and, where necessary, the nesting of subschemas. Such constructed names are not guranteed to be unique though, so use of `$javaType` may be required to provide uniqueness in some circumstances.



## Schema references (`$ref`)

The `$ref` keyword is supported:
* to a subschema within the same file using a JSON pointer fragment URI, such as `$ref: #/definitions/Foo`
* to a schema (or subschema) that's available in a JSON or YAML file on the source path using a relative URI in the `$ref`, such as `$ref: my-other-file.json#`
* to a schema (or subschema) that's available in a JAR file on the classpath (i.e. from a previous run of this tooling). This allows you to compile schemas in different projects and treat them like normal Java dependencies. For example if you have a schema with root object with `id: https://example.com/schema/foo` and you compile that, and then you can add the jar to the classpath of another project and reference it from the schema in that other project using `$ref: https://example.com/schema/foo`.

The tool never accesses the network in order to resolve a `$ref`. 
# TODO

## Theme: make more sympathetic to Json Schema:
- `patternProperties`/`additionalProperties` selection — the original complaint that kicked
  off this whole design exercise. Still open.
- Array element insertion/deletion — needs a design pass first; no precedent for what
  "insert into an array" means yet.
- Real type-union support on `SchemaConfig.type` (currently a plain `String`).
- Understand JsonSchema better -- e.g. `$refs`, and the way linking works. 

## Theme: generalize to other schema languages
- Avro: `record`/`array`/`string`/`int` masking now works (`avro/AvroFunction`, built directly
  from a real `org.apache.avro.Schema` rather than a shadow config model — see the module
  README). Still open:
  - Unions/nullability (`type: [..., "null"]`) — needs a design pass, same as JSON's own
    still-open type-union support above.
  - Every other Avro type: `map`, `enum`, `fixed`, `bytes`, `boolean`, `long`, `float`, `double`.
  - Generation and delete/insert of fields — meaningless without union/default support first,
    so currently fail loudly rather than silently producing non-conforming records.
  - Wiring into a real `Filter` (see "Theme: An actual Filter" below) — applies equally to JSON.
- We need to avoid taking decisions which won't work for protobuf, or at least not without awareness that we're making such a decision.
- Make data formats a pluggable abstraction. 
  
## Theme: An actual Filter
- Write the code to turn this into a real `Filter`
- Figure out the Filter's configuration. This gets tricky, because once you're beyond demo-ware you need to treat masks themselves as entities, and so then you want schema registry support.
    ```yaml
    - topic:
        value: bill-events
      apply: 
      - if: 
          recordHeader: # ...
          headerKey: 
            value: my-header
          headerValue: 
            value: blah # optional
        then:
      recordKey: # ...
      - if: 
          recordValue:
          - hasSchemaId: 
              value: 123
              atLocation: prefix # optional, if we want to support apicurio headers, but default to the confluent 4 byte prefix
        then: 
          apply
            - schemaValidation: 
            - signatureValidation
            - if: 
                recordDirection: out # in=Produce, out=Fetch (and ShareFetch), in,out=both and out,in (e.g. useful for symmetric transformation like record- and field-level encryption)
                clientSubject:
                  value: 
                    principal: User
                    name: BillingTeam
                clientId: 
                  value: billing-app
              then:     
                - mask:
            - recordEncryption:
            - compression: 
    ````

## Theme: More functions
- A general find/replace for `String` data
    ```yaml
    replaceAll:
      pattern: <regex-with-groups>
      replacement: <replacenent-strings-with-group-placeolders>
      groups: # optional
        - groupName: # the name of a capturing group in the `pattern`, which is also present in the `replacement`
          apply: # another `ContextPipeline` to transform the captured groups prior to the `replacement` interpolation
          - value: REDACTED
              
    ```
    Similarly `replaceFirst`
- A function with `try`/`catch` -like semantics.
- E.g. add an absolute/relative error to a number value
- Make transformations a pluggable abstraction. 
- LocalDate, LocalDateTime, LocalTime, ZonedDateTime, Instant, Duration, etc.

## Theme: Schema Registry integration

## Theme: Tech debt

- root-level apply: [delete] semantics (undefined — "delete the whole record" is a
  bigger question), 
- cosmetic *MaskConfig renames (these classes are all JSON specific really). 
- YAML anchor/alias blow-up guard (just don't allow YAML, only JSON)
- Recursion depth limit (avoid StackOverflowException on deeply nested data)

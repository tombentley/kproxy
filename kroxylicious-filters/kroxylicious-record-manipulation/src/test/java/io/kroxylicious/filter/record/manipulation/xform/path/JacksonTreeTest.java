/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonTreeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Test
    void test() throws Exception {
        var example = MAPPER.readTree("""
                { "store": {
                    "book": [
                      { "category": "reference",
                        "author": "Nigel Rees",
                        "title": "Sayings of the Century",
                        "price": 8.95
                      },
                      { "category": "fiction",
                        "author": "Evelyn Waugh",
                        "title": "Sword of Honour",
                        "price": 12.99
                      },
                      { "category": "fiction",
                        "author": "Herman Melville",
                        "title": "Moby Dick",
                        "isbn": "0-553-21311-3",
                        "price": 8.99
                      },
                      { "category": "fiction",
                        "author": "J. R. R. Tolkien",
                        "title": "The Lord of the Rings",
                        "isbn": "0-395-19395-8",
                        "price": 22.99
                      }
                    ],
                    "bicycle": {
                      "color": "red",
                      "price": 399
                    }
                  }
                }
                """);
        JacksonTree jacksonTree = new JacksonTree();
//        jacksonTree.eval(example, List.of(new Path(Identifier.ROOT,
//                List.of(new Segment.Descendant(new Selector.Name("price"))),
//                node -> System.out.println(node))));

//        jacksonTree.eval(example, List.of(new Path(Identifier.ROOT,
//                List.of(new Segment.Descendant(new Selector.Name("book")),
//                        new Segment.Child(List.of(new Selector.Index(0), new Selector.Index(1))),
//                        new Segment.Child(new Selector.Name("author"))),
//                node -> System.err.println(node))));
    }

}

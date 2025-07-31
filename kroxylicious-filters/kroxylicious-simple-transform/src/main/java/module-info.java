module io.kroxylicious.filter.simpletransformation {
    requires com.fasterxml.jackson.annotation;
    requires com.github.spotbugs.annotations;
    requires kafka.clients;
    requires org.slf4j;
    requires kroxylicious.api;
    uses io.kroxylicious.proxy.filter.simpletransform.ByteBufferTransformationFactory;
    provides io.kroxylicious.proxy.filter.simpletransform.ByteBufferTransformationFactory
            with io.kroxylicious.proxy.filter.simpletransform.UpperCasing,
                    io.kroxylicious.proxy.filter.simpletransform.Replacing;
}
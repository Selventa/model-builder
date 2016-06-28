package model.builder.common

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.TupleConstructor

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT

/**
 * Provides json-stream parsing capabilities.
 *
 * <p>
 * I need to be bootstrapped with {@link JsonStream#initializeFactory()} to set
 * up the {@link JsonFactory}.  This should only be done once as the
 * {@link JsonFactory} is reusable and thread-safe.
 * </p>
 */
@Singleton
class JsonStream {

    private static final TypeReference mapRef = new TypeReference<Map<String, Object>>() {}
    private JsonFactory factory

    synchronized def initializeFactory() {
        ObjectMapper mapper = new ObjectMapper()
        factory = mapper.factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
    }

    /**
     * Return an {@link Iterator} of json object {@link Map maps}.
     *
     * @param stream {@link InputStream}
     * @return
     */
    def Iterator<Map> jsonObjects(InputStream stream) {
        return new JsonIterator(factory.createParser(stream))
    }

    /**
     * Iterate json objects as {@link Map maps} from the {@link JsonParser}.
     *
     * <p>
     * Not thread-safe.
     * </p>
     */
    @TupleConstructor
    final class JsonIterator implements Iterator<Map>, Closeable {

        final JsonParser parser
        boolean available
        Map current

        /**
         * {@inheritDoc}
         */
        @Override
        boolean hasNext() {
            available || advance()
        }

        /**
         * {@inheritDoc}
         */
        @Override
        Map next() {
            if (!available && !advance()) throw new NoSuchElementException()
            available = false
            current
        }

        /**
         * Always advances to the next available json object {@link Map map}.
         *
         * @return {@code true} if a json object is available; {@code false} if not
         */
        private boolean advance() {
            JsonToken token
            while ((token = parser.nextToken()) != null) {
                switch (token) {
                    case START_OBJECT:
                        current = parser.readValueAs(mapRef)
                        return available = true
                }
            }
            return available = false
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void remove() {
            throw new UnsupportedOperationException()
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void close() throws IOException {
            parser.close()
        }
    }
}

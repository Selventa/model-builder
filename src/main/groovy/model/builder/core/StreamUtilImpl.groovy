package model.builder.core

import org.cytoscape.io.util.StreamUtil

import java.nio.file.Files
import java.nio.file.Paths

class StreamUtilImpl implements StreamUtil {

    @Override
    InputStream getInputStream(String source) throws IOException {
        toStream(source)
    }

    @Override
    InputStream getInputStream(URL source) throws IOException {
        toStream(source)
    }

    @Override
    URLConnection getURLConnection(URL url) throws IOException {
        url.openConnection()
    }

    static InputStream toStream(source) {
        if (Files.exists(Paths.get(source.toString()))) {
            return new FileInputStream(new File(source.toString()))
        }
        def url = new URL(source.toString())
        url.openStream()
    }
}

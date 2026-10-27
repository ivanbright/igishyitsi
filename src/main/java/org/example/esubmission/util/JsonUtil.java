package org.example.esubmission.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared Jackson configuration used to serialize admin page projections.
 */
public final class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtil() {}

    /**
     * Makes a JSON document safe to embed inside an HTML
     * {@code <script type="application/json">} block.
     * <p>
     * Without this, any user-supplied value containing {@code </script>} (for
     * example a class name or assignment title) closes the script element
     * early, corrupting the payload and breaking {@code JSON.parse} on the
     * client. Escaping {@code <}, {@code >} and {@code &} as unicode escapes
     * keeps the value valid JSON while making it inert as markup.
     * </p>
     *
     * @param json serialized JSON, may be {@code null}
     * @return JSON that is safe to embed in a script element
     */
    public static String forScriptEmbedding(String json) {
        if (json == null) {
            return "{}";
        }
        return json.replace("&", "\\u0026")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e");
    }
}
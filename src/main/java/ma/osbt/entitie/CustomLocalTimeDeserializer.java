package ma.osbt.entitie;

 
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalTime;

public class CustomLocalTimeDeserializer extends StdDeserializer<LocalTime> {

    public CustomLocalTimeDeserializer() {
        super(LocalTime.class);
    }

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String timeStr = p.getText();

        // Si la chaîne contient 3 ":" (ex: 10:00:00:00), on enlève le dernier segment
        if (timeStr.chars().filter(ch -> ch == ':').count() > 2) {
            timeStr = timeStr.substring(0, timeStr.lastIndexOf(':'));
        }

        // Désérialisation classique
        return LocalTime.parse(timeStr);
    }
}


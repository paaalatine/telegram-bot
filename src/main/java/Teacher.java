import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Teacher {
    static Bot bot = new Bot();
    static String REQUEST_FILE = "/home/sonya/Downloads/dialog_converter/question";
    static String RESPONSE_FILE = "/home/sonya/Downloads/dialog_converter/answer";

    @SneakyThrows
    public static void main(String[] args) {
        try (BufferedReader reader1 = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(REQUEST_FILE), Charset.forName("UTF-8")));
             BufferedReader reader2 = new BufferedReader(
                     new InputStreamReader(
                             new FileInputStream(RESPONSE_FILE), Charset.forName("UTF-8")))) {

            String line1, line2;
            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null)
                bot.teach(line1, line2);
        }
    }
}


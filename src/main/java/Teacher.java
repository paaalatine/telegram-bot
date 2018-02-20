import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Teacher {
    static Bot bot = new Bot();
    static String input;
    static String output;


public static void main(String[] args) throws IOException {

    BufferedReader reader = null;
    try {
        reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream("/home/sonya/dialogue"), Charset.forName("UTF-8")));
        String line;
        while ((line = reader.readLine()) != null) {
            input = output;
            if(input == null)
                input ="";
            output = line;
            bot.teach(input,output);
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // log warning
            }
        }
    }
}
}


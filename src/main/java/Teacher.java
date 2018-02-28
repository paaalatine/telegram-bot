import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Teacher {
    private static Bot bot = new Bot();

    @SneakyThrows
    public static void main(String[] args) {
        try (BufferedReader reader1 = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream("/home/sonya/Downloads/dialog_converter/question"), Charset.forName("UTF-8")));
             BufferedReader reader2 = new BufferedReader(
                     new InputStreamReader(
                             new FileInputStream("/home/sonya/Downloads/dialog_converter/answer"), Charset.forName("UTF-8")))) {

            String line1, line2;
            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null)
                bot.teach(line1, line2);
        }
    }
}


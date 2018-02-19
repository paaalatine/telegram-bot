import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BotTest {
    private Bot bot;

    @Before
    public void init() { bot = new Bot(); }
    @After
    public void tearDown() { bot = null; }

    @Test
    public void getWords(){
        List<String> right = new ArrayList<>();
        right.add("hello");
        right.add("how");
        right.add("be");
        right.add("you");
        assertEquals(right, bot.getWords("Hello. How are you?"));
    }

    public static void main(String[] args) {
        JUnitCore runner = new JUnitCore();
        Result result = runner.run(BotTest.class);
        System.out.println("run tests: " + result.getRunCount());
        System.out.println("failed tests: " + result.getFailureCount());
        System.out.println("ignored tests: " + result.getIgnoreCount());
        System.out.println("success: " + result.wasSuccessful());
    }
}

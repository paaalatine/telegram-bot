import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot {
    public static void main(String[] args) {
        ApiContextInitializer.init();
        JDBCPostgreSQL.connect();
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            botapi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "paaalatine_bot";
    }

    @Override
    public void onUpdateReceived(Update e) {
        Message msg = e.getMessage();
        String txt = msg.getText();
        String username = msg.getChat().getUserName();
        if (txt.equals("/start")) {
            sendMsg(msg, "Hello, " + username + "!");
        }
    }

    @Override
    public String getBotToken() {
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get("src/main/resources/token.txt")));
        } catch (IOException e) { content = null; }
        return content;
    }

    @SuppressWarnings("deprecation") // Означает то, что в новых версиях метод уберут или заменят
    private void sendMsg(Message msg, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(msg.getChatId());
        s.setText(text);
        try {
            sendMessage(s);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    private void setAssociations(String input, String output){

    }

    public List<String> getWords(String msg){
        msg = msg.replaceAll("\\p{Punct}", "");
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(msg);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        List<String> lemmas = new LinkedList<>();
        for(CoreMap sentence : sentences){
            for(CoreLabel word : sentence.get(CoreAnnotations.TokensAnnotation.class)){
                lemmas.add(word.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        List<String> result = new ArrayList<>();
        result.addAll(lemmas);

        /*TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
        factory = new EnglishStopTokenizerFactory(factory);
        Tokenizer tokenizer = factory.tokenizer(result.toString().toCharArray(), 0, msg.length());
        for(String token : tokenizer){
            result.add(token);
        }*/
        return result;
    }
}

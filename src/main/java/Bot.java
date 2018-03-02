import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.InputStream;
import java.util.*;


@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bot extends TelegramLongPollingBot {

    final String botUsername;
    final String botToken;
    final JdbcPostgreSql db;

    String lastBotReplica;
    String humanResponse;
    boolean teach;

    @SneakyThrows
    public Bot() {
        Properties properties = new Properties();
        try(InputStream inputStream = Bot.class.getResourceAsStream("/bot.properties")) {
            properties.load(inputStream);
        }
        lastBotReplica = properties.getProperty("lastBotReplica", "");
        humanResponse = properties.getProperty("humanResponse", "");
        botUsername = Optional.ofNullable(properties.getProperty("botUsername"))
                .orElseThrow(() -> new RuntimeException("Botname is not found! " +
                                                        "Specify it in \"bot.properties\"."));
        botToken = Optional.ofNullable(properties.getProperty("botToken"))
            .orElseThrow(() -> new RuntimeException("Token is not found! " +
                                                    "Specify it in \"bot.properties\"."));
        db = new JdbcPostgreSql();
    }

    @SneakyThrows
    public static void main(String[] args) {
        ApiContextInitializer.init();
        new TelegramBotsApi().registerBot(new Bot());
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update e) {
        Message recievedMsg = e.getMessage();
        String msgText = recievedMsg.getText();
        humanResponse = msgText != null ? msgText : "";
        if (teach)
            teachAndThank(recievedMsg);
        else {
            List<String> humanWords = getWords(humanResponse);
            int[] humanWordsId = new int[humanWords.size()];
            for(int i = 0; i < humanWords.size(); i++)
                humanWordsId[i] = db.addWord(humanWords.get(i));
            int sentenceId = db.getResponse(humanWordsId, recievedMsg.getChat().getUserName());
            if (sentenceId == 0)
                askToTeach(recievedMsg);
            else
                answer(recievedMsg, sentenceId);
        }
    }

    private void answer(Message msg, int sentenceId) {
        db.addUsedSentence(msg.getChat().getUserName(), sentenceId);
        teach(lastBotReplica, humanResponse);
        lastBotReplica = db.getSentence(sentenceId);
        sendMsg(msg, lastBotReplica);
    }

    private void teachAndThank(Message msg) {
        teach(lastBotReplica, humanResponse);
        lastBotReplica = "Thank you.";
        sendMsg(msg, lastBotReplica);
        lastBotReplica = "";
        teach = false;
    }

    private void askToTeach(Message msg) {
        lastBotReplica = "I don't understand. Teach me what I should say in this situation, please.";
        teach = true;
        sendMsg(msg, lastBotReplica);
        lastBotReplica = humanResponse;
    }

    @SuppressWarnings("deprecation")
    @SneakyThrows
    private void sendMsg(Message msg, String text) {
        sendMessage(new SendMessage()
                .setChatId(msg.getChatId())
                .setText(text));
    }

    private void setAssociations(List<String> replicaWords, String response) {
        int sentenceId = db.addSentence(response);
        int wordId, associationId, sentenceLength = 0;
        for(String word : replicaWords)
           sentenceLength += word.length();
        for (String word : replicaWords) {
            wordId = db.addWord(word);
            double oldWeight = db.getAssociationWeight(wordId, sentenceId);
            double newWeight = (oldWeight * sentenceLength + 1)/sentenceLength;
            db.updateWord(wordId);
            associationId = db.addAssociation(wordId, sentenceId);
            db.updateAssociation(associationId, newWeight);
        }
    }

    public void teach(String replica, String response){
        List<String> replicaWords = getWords(replica);
        setAssociations(replicaWords, response);

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
        for(CoreMap sentence : sentences)
            for(CoreLabel word : sentence.get(CoreAnnotations.TokensAnnotation.class))
                lemmas.add(word.get(CoreAnnotations.LemmaAnnotation.class));
        return new ArrayList<>(lemmas);
    }
}

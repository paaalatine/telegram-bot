import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import lombok.SneakyThrows;
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

    private String lastBotReplica = "";
    private String humanResponse = "";
    private boolean teach = false;
    private static final String botUsername = "paaalatine_bot";
    private static final JDBCPostgreSQL db = new JDBCPostgreSQL();

    @SneakyThrows
    public static void main(String[] args) throws TelegramApiException{
        ApiContextInitializer.init();
        new TelegramBotsApi().registerBot(new Bot());
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update e) {
        Message recievedMsg = e.getMessage();
        humanResponse = recievedMsg.getText();
        if (teach){
            teach(lastBotReplica, humanResponse);
            lastBotReplica = "Thank you.";
            sendMsg(recievedMsg, lastBotReplica);
            lastBotReplica = "";
            teach = false;
        }
        else {
            List<String> humanWords = getWords(humanResponse);
            int[] humanWordsId = new int[humanWords.size()];
            for(int i = 0; i < humanWords.size(); i++) {
                humanWordsId[i] = db.addWord(humanWords.get(i));
            }
            int sentenceId = db.getResponse(humanWordsId);
            if (sentenceId == 0) {
                lastBotReplica = "I don't understand. Teach me what I should say in this situation, please.";
                teach = true;
                sendMsg(recievedMsg, lastBotReplica);
                lastBotReplica = humanResponse;
            }
            else {
                teach(lastBotReplica, humanResponse);
                lastBotReplica = db.getSentence(sentenceId);
                sendMsg(recievedMsg, lastBotReplica);
            }
        }
    }

    @Override
    public String getBotToken() {
        String content;
        try {
            content = "521024889:AAF75zgck9a6LeMM4IUeglsOcfAtlEgzb_Y";
        } catch (Exception e) { content = null; }
        return content;
    }

    @SuppressWarnings("deprecation")
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

    private void setAssociations(List<String> replicaWords, String response){
        int sentenceId = db.addSentence(response);
        int wordId, associationId, sentenceLength = 0;
        for(String word : replicaWords){
           sentenceLength += word.length();
        }
        for (String word : replicaWords){
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
        for(CoreMap sentence : sentences){
            for(CoreLabel word : sentence.get(CoreAnnotations.TokensAnnotation.class)){
                lemmas.add(word.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return new ArrayList<>(lemmas);
    }
}

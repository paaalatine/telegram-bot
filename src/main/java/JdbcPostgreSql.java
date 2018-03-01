import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.sql.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JdbcPostgreSql {

    HikariDataSource ds;

    @SneakyThrows
    public JdbcPostgreSql() {
        HikariConfig cfg = new HikariConfig("/db.properties");
        ds = new HikariDataSource(cfg);
    }

    @SneakyThrows
    public int addSentence(String text) {
        String SELECT_QUERY = "SELECT id FROM sentence WHERE text = ?";
        String INSERT_QUERY = "INSERT INTO sentence (text) VALUES (?) RETURNING id";
        try (Connection con = ds.getConnection();
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY);
             PreparedStatement insertSt = con.prepareStatement(INSERT_QUERY)) {

            selectSt.setString(1, text);
            ResultSet rs = selectSt.executeQuery();

            if (!rs.next()) {
                insertSt.setString(1, text);
                rs = insertSt.executeQuery();
                rs.next();
            }
            int id = rs.getInt(1);
            rs.close();
            return id;
        }
    }

    @SneakyThrows
    public String getSentence(int id) {
        String SELECT_QUERY = "SELECT text FROM sentence WHERE id = ?";
        try (Connection con = ds.getConnection();
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY)) {
            selectSt.setInt(1, id);
            ResultSet rs = selectSt.executeQuery();
            rs.next();
            String text = rs.getString(1);
            rs.close();
            return text;
        }
    }

    @SneakyThrows
    public int addWord(String text) {
        String SELECT_QUERY = "SELECT id FROM word WHERE text = ?";
        String INSERT_QUERY = "INSERT INTO word (text, weight) VALUES (?, ?) RETURNING id";
        int defaultWeight = 1;
        try (Connection con = ds.getConnection();
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY);
             PreparedStatement insertSt = con.prepareStatement(INSERT_QUERY)) {

            selectSt.setString(1, text);
            ResultSet rs = selectSt.executeQuery();

            if (!rs.next()) {
                rs = insert(insertSt, text, defaultWeight);
                rs.next();
            }
            int id = rs.getInt(1);
            rs.close();
            return id;
        }
    }

    @SneakyThrows
    public void updateWord(int id) {
        String UPDATE_QUERY = "UPDATE word SET weight = weight/(weight + 1) WHERE id = " + id;
        try (Connection con = ds.getConnection();
             PreparedStatement updateSt = con.prepareStatement(UPDATE_QUERY)) {
            updateSt.executeUpdate();
        }
    }

    @SneakyThrows
    public int addAssociation(int wordId, int sentenceId) {
        String SELECT_QUERY = "SELECT id, weight FROM association WHERE word_id = ? AND sentence_id = ?";
        String INSERT_QUERY = "INSERT INTO association (word_id, sentence_id, weight) VALUES (?, ?, ?) RETURNING id";
        int defaultWeight = 0;
        try (Connection con = ds.getConnection();
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY);
             PreparedStatement insertSt = con.prepareStatement(INSERT_QUERY)) {

            selectSt.setInt(1, wordId);
            selectSt.setInt(2, sentenceId);
            ResultSet rs = selectSt.executeQuery();

            if (!rs.next()) {
                insertSt.setInt(1, wordId);
                insertSt.setInt(2, sentenceId);
                insertSt.setDouble(3, defaultWeight);
                rs = insertSt.executeQuery();
                rs.next();
            }
            int id = rs.getInt(1);
            rs.close();
            return id;
        }
    }

    @SneakyThrows
    public void updateAssociation(int associationId, double weight) {
        String UPDATE_QUERY = "UPDATE association SET weight = ? WHERE id = ?";
        try (Connection con = ds.getConnection();
             PreparedStatement updateSt = con.prepareStatement(UPDATE_QUERY)) {
            updateSt.setDouble(1, weight);
            updateSt.setInt(2, associationId);
            updateSt.executeUpdate();
        }
    }

    @SneakyThrows
    public double getAssociationWeight(int wordId, int sentenceId) {
        String SELECT_QUERY = "SELECT weight FROM association WHERE word_id = ? AND sentence_id = ?";
        try (Connection con = ds.getConnection();
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY)) {
            selectSt.setInt(1, wordId);
            selectSt.setInt(2, sentenceId);
            ResultSet rs = selectSt.executeQuery();
            double weight = 0;
            if(rs.next())
                weight = rs.getDouble(1);
            rs.close();
            return weight;
        }
    }

    @SneakyThrows
    private ResultSet insert (PreparedStatement st, String string, int id) {
        st.setString(1, string);
        st.setInt(2, id);
        ResultSet rs = st.executeQuery();
        return rs;
    }

    @SneakyThrows
    public void addUsedSentence(String interlocutor, int sentenceId) {
        String SELECT_QUERY = "SELECT id FROM chat WHERE interlocutor = ? AND sentence_id = ?";
        String INSERT_QUERY = "INSERT INTO chat (interlocutor, sentence_id, times) VALUES (?, ?, 1) RETURNING id";
        String UPDATE_QUERY = "UPDATE chat SET times = times + 1 WHERE id = ?";
        try (Connection con = ds.getConnection();
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY);
             PreparedStatement insertSt = con.prepareStatement(INSERT_QUERY);
             PreparedStatement updateSt = con.prepareStatement(UPDATE_QUERY)) {

            selectSt.setString(1, interlocutor);
            selectSt.setInt(2, sentenceId);
            ResultSet rs = selectSt.executeQuery();

            if (!rs.next()) {
                rs = insert(insertSt, interlocutor, sentenceId);
                rs.next();
            }
            updateSt.setInt(1, rs.getInt(1));
        }
    }

    @SneakyThrows
    public int getResponse(int[] replicaWordsId, String userName) {
        String DELETE_QUERY = "DELETE FROM temp";
        String INSERT_QUERY = "INSERT INTO temp SELECT association.sentence_id, association.weight * word.weight / (SELECT COALESCE(MAX(times), 1) FROM chat WHERE chat.interlocutor = ? AND chat.sentence_id = association.sentence_id) FROM association " +
                "JOIN word ON association.word_id = word.id " +
                "WHERE word_id = ?";
        String SELECT_QUERY = "SELECT sentence_id, SUM(weight) FROM temp GROUP BY sentence_id ORDER BY SUM(weight) DESC";
        try (Connection con = ds.getConnection();
             PreparedStatement deleteSt = con.prepareStatement(DELETE_QUERY);
             PreparedStatement insertSt = con.prepareStatement(INSERT_QUERY);
             PreparedStatement selectSt = con.prepareStatement(SELECT_QUERY)) {

            deleteSt.executeUpdate();
            for (int wordId : replicaWordsId) {
                insertSt.setString(1, userName);
                insertSt.setInt(2, wordId);
                insertSt.executeUpdate();
            }

            ResultSet rs = selectSt.executeQuery();
            int id = 0;
            if(rs.next())
                id = rs.getInt(1);
            rs.close();
            return id;
        }
    }
}

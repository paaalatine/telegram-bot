import java.sql.*;
import java.util.List;

public class JDBCPostgreSQL {

    private static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/sonya";
    private static final String USER = "sonya";
    private static final String PASS = "xkl088";
    private static Connection db;

    private void connect() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver is not found.");
            e.printStackTrace();
            return;
        }

        System.out.println("PostgreSQL JDBC Driver successfully connected");

        try {
            db = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            System.out.println("Connection Failed");
            e.printStackTrace();
            return;
        }

        if (db != null) {
            System.out.println("You successfully connected to database now");
        } else {
            System.out.println("Failed to make connection to database");
        }
    }

    private void closeAll(Connection c, ResultSet r, PreparedStatement s1, PreparedStatement s2) {
        try {
            if (s1 != null)
                s1.close();
            if (s2 != null)
                s2.close();
            if (r != null)
                r.close();
            if (c != null)
                c.close();
        } catch (SQLException e) { }
    }

    public int addSentence(String text) {
        int rowCounter = 0;
        PreparedStatement selectSt = null;
        PreparedStatement insertSt = null;
        ResultSet rs = null;
        try {
            connect();
            selectSt = db.prepareStatement("SELECT id FROM sentence WHERE text = ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            insertSt = db.prepareStatement("INSERT INTO sentence (text) VALUES (?) RETURNING id");
            selectSt.setString(1, text);
            rs = selectSt.executeQuery();

            while (rs.next()) rowCounter++;

            if (rowCounter == 0) {
                insertSt.setString(1, text);
                rs = insertSt.executeQuery();
                rs.next();
            } else
                rs.first();
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        } finally {
            closeAll(db, rs, selectSt, insertSt);
        }
    }

    public String getSentence(int id) {
        PreparedStatement selectSt = null;
        ResultSet rs = null;
        try {
            connect();
            selectSt = db.prepareStatement("SELECT text FROM sentence WHERE id = ?");
            selectSt.setInt(1, id);
            rs = selectSt.executeQuery();
            rs.next();
            return rs.getString(1);
        } catch (SQLException e) {
            return "";
        }
        finally {
            closeAll(db, rs, selectSt, null);
        }
    }

    public int addWord(String text) {
        int rowCounter = 0;
        int defaultWeight = 1;
        PreparedStatement selectSt = null;
        PreparedStatement insertSt = null;
        ResultSet rs = null;
        try {
            connect();
            selectSt = db.prepareStatement("SELECT id FROM word WHERE text = ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            insertSt = db.prepareStatement("INSERT INTO word (text, weight) VALUES (?, ?) RETURNING id");

            selectSt.setString(1, text);
            rs = selectSt.executeQuery();

            while (rs.next()) rowCounter++;

            if (rowCounter == 0) {
                insertSt.setString(1, text);
                insertSt.setInt(2, defaultWeight);
                rs = insertSt.executeQuery();
                rs.next();
            } else
                rs.first();;
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
        finally {
            closeAll(db, rs, selectSt, insertSt);
        }
    }

    public void updateWord(int id) {
        PreparedStatement st = null;
        try {
            connect();
            st = db.prepareStatement("UPDATE word SET weight = weight/(weight + 1) WHERE id = " + id);
            st.executeUpdate();
        } catch (SQLException e) { }
        finally {
            closeAll(db, null, st, null);
        }
    }

    public int addAssociation(int wordId, int sentenceId) {
        int rowCounter = 0;
        int defaultWeight = 0;
        PreparedStatement selectSt = null;
        PreparedStatement insertSt = null;
        ResultSet rs = null;
        try {
            connect();
            selectSt = db.prepareStatement("SELECT id, weight FROM association WHERE word_id = ? AND sentence_id = ?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            insertSt = db.prepareStatement("INSERT INTO association (word_id, sentence_id, weight) VALUES (?, ?, ?) RETURNING id");
            selectSt.setInt(1, wordId);
            selectSt.setInt(2, sentenceId);
            rs = selectSt.executeQuery();

            while (rs.next()) rowCounter++;

            if (rowCounter == 0) {
                insertSt.setInt(1, wordId);
                insertSt.setInt(2, sentenceId);
                insertSt.setDouble(3, defaultWeight);
                rs = insertSt.executeQuery();
                rs.next();
            } else
                rs.first();
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
        finally {
            closeAll(db, rs, selectSt, insertSt);
        }
    }

    public void updateAssociation(int associationId, double weight) {
        PreparedStatement st = null;
        try {
            connect();
            st = db.prepareStatement(String.format("UPDATE association SET weight = ? WHERE id = ?", weight, associationId));
            st.setDouble(1, weight);
            st.setInt(2, associationId);
            st.executeUpdate();
        } catch (SQLException e) { }
        finally {
            closeAll(db, null, st, null);
        }
    }

    public double getAssociationWeight(int wordId, int sentenceId) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            connect();
            st = db.prepareStatement("SELECT weight FROM association WHERE word_id = ? AND sentence_id = ?");
            st.setInt(1, wordId);
            st.setInt(2, sentenceId);
            rs = st.executeQuery();
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            return 0;
        } finally {
            closeAll(db, rs, st, null);
        }
    }

    public void addChat(String interlocutor, int sentenceId) {
        PreparedStatement st = null;
        try {
            connect();
            st = db.prepareStatement("INSERT INTO chat (interlocutor, sentence_id) VALUES (?, ?)");
            st.setString(1, interlocutor);
            st.setInt(2, sentenceId);
            st.executeUpdate();
        } catch (SQLException e) { }
        finally {
            closeAll(db, null, st, null);
        }
    }

    public int getResponse(int[] replicaWordsId){
        PreparedStatement deleteSt = null;
        PreparedStatement selectSt = null;
        ResultSet rs = null;
        try {
            connect();
            deleteSt = db.prepareStatement("DELETE FROM temp");
            deleteSt.executeUpdate();
            for(int wordId: replicaWordsId){
                PreparedStatement insertSt = db.prepareStatement("INSERT INTO temp SELECT association.sentence_id, association.weight * word.weight FROM association JOIN word ON association.word_id = word.id WHERE word_id = ?");
                insertSt.setInt(1, wordId);
                insertSt.executeUpdate();
                insertSt.close();
            }
            selectSt = db.prepareStatement("SELECT sentence_id, SUM(weight) FROM temp GROUP BY sentence_id HAVING SUM(weight) >= 0.01 ORDER BY SUM(weight) DESC");
            rs = selectSt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            closeAll(db, rs, deleteSt, selectSt);
        }
    }
}

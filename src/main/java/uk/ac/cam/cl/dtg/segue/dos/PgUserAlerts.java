package uk.ac.cam.cl.dtg.segue.dos;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PgUserAlerts implements IUserAlerts {

    private final PostgresSqlDb db;

    @Inject
    public PgUserAlerts(final PostgresSqlDb db) {
        this.db = db;
    }

    private PgUserAlert buildPgUserAlert(final ResultSet result) throws SQLException {
        return new PgUserAlert(
                result.getLong("id"),
                result.getLong("user_id"),
                result.getString("message"),
                result.getString("link"),
                result.getDate("created"),
                result.getDate("seen"),
                result.getDate("clicked"),
                result.getDate("dismissed"));
    }

    @Override
    public List<IUserAlert> getUserAlerts(Long userId) throws SegueDatabaseException {
        try (Connection conn = db.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM user_alerts WHERE user_id = ? ORDER BY created ASC");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();
            List<IUserAlert> returnResult = Lists.newArrayList();
            while (results.next()) {
                returnResult.add(buildPgUserAlert(results));
            }
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public IUserAlert createAlert(Long userId, String message, String link) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = db.getDatabaseConnection()) {

            pst = conn
                    .prepareStatement("INSERT INTO user_alerts "
                            + "(user_id, message, link) "
                            + "VALUES (?, ?, ?) RETURNING *");

            pst.setLong(1, userId);
            pst.setString(2, message);
            pst.setString(3, link);

            ResultSet results = pst.executeQuery();
            results.next();
            return buildPgUserAlert(results);

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void recordAlertEvent(Long alertId, IUserAlert.AlertEvents eventType) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = db.getDatabaseConnection()) {

            String q = "UPDATE user_alerts SET ";
            switch (eventType) {
                case SEEN:
                    q += "seen";
                    break;
                case CLICKED:
                    q += "clicked";
                    break;
                case DISMISSED:
                    q += "dismissed";
                    break;
            }
            q += " WHERE id = ?";
            pst = conn
                    .prepareStatement(q);

            pst.setLong(1, alertId);

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to update user notification.");
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}

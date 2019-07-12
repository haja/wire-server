package at.sbaresearch.mqtt4android.integration.wire.jdbc;

import at.sbaresearch.mqtt4android.integration.wire.RelayConnection;
import at.sbaresearch.mqtt4android.integration.wire.RegistrationRepository;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Repository
public class JdbcRegistrationRepository implements RegistrationRepository {

  //language=SQL
  private static final String INSERT_CONNECTION =
      "INSERT INTO relay_connection(token, relay_url, relay_cert) VALUES (:token, :relayUrl, :relayCert)";
  //language=SQL
  private static final String SELECT_CONNECTION =
      "SELECT relay_url, relay_cert FROM relay_connection WHERE token=:token";

  Jdbi jdbi;

  @Override
  public void addDeviceRelay(String token, RelayConnection connection) {
    // TODO should re-registering drop old token for the same app and device?
    jdbi.useHandle(h -> h.createUpdate(INSERT_CONNECTION)
        .bind("token", token)
        .bind("relayUrl", connection.getRelayUrl())
        .bind("relayCert", connection.getRelayCert())
        .execute());
  }

  @Override
  public Option<RelayConnection> getDeviceRelay(String token) {
    RowMapper<RelayConnection> mapper = (rs, ctx) ->
        new RelayConnection(
            rs.getString("relay_url"),
            rs.getBytes("relay_cert")
        );
    return Option.ofOptional(jdbi.withHandle(h -> h.select(SELECT_CONNECTION)
        .bind("token", token)
        .map(mapper)
        .findFirst()));
  }
}

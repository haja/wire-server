package at.sbaresearch.mqtt4android.integration.wire;

import io.vavr.control.Option;

public interface RegistrationRepository {
  void addDeviceRelay(String token, RelayConnection registration);
  Option<RelayConnection> getDeviceRelay(String token);
}

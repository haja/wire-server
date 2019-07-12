package at.sbaresearch.mqtt4android.integration.wire;

import lombok.Value;

@Value
public class RelayConnection {
  String relayUrl;
  byte[] relayCert;
}

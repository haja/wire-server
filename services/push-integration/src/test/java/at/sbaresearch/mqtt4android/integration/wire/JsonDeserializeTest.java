/*
 * Wire Push Integration for Push Adapter
 * Copyright (c) 2020 Harald Jagenteufel.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package at.sbaresearch.mqtt4android.integration.wire;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JsonDeserializeTest {

  @Autowired
  ObjectMapper mapper;
  private final String json = "{ \"prop\": \"value\", \"prop2\": \"value2\" }";
  // FIXME this is not working (one prop on json)
  //  Jackson then tries unwrapping or something...
  //  see
  private final String jsonSingle = "{ \"singleProp\": \"singleValue\" }";

  @Test
  public void pojoValueTest() throws IOException {
    TestPojo x = mapper.readValue(json, TestPojo.class);
    assertThat(x).isNotNull();
    assertThat(x.getProp()).isEqualTo("value");
    assertThat(x.getProp2()).isEqualTo("value2");
  }

  @Test
  public void lombokValueTest() throws IOException {
    LombokPojo x = mapper.readValue(json, LombokPojo.class);
    assertThat(x).isNotNull();
    assertThat(x.getProp()).isEqualTo("value");
    assertThat(x.getProp2()).isEqualTo("value2");
  }

}

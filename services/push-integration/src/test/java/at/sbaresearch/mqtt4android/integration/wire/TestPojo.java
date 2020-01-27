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

public class TestPojo {
  private final String prop;
  private final String prop2;

  //@java.beans.ConstructorProperties({"prop", "prop2"})
  public TestPojo(String prop, String prop2) {
    this.prop = prop;
    this.prop2 = prop2;
  }

  public String getProp2() {
    return prop2;
  }

  public String getProp() {
    return this.prop;
  }


}

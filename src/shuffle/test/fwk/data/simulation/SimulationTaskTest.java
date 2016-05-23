/*  ShuffleMove - A program for identifying and simulating ideal moves in the game
 *  called Pokemon Shuffle.
 *  
 *  Copyright (C) 2015  Andrew Meyers
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package shuffle.test.fwk.data.simulation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import shuffle.fwk.data.simulation.SimulationTask;

/**
 * @author Andrew Meyers
 *
 */
public class SimulationTaskTest {
   
   /**
    * Test method for {@link shuffle.fwk.data.simulation.SimulationTask#getComboMultiplier(int)}.
    */
   @Test
   public final void testGetComboMultiplier() {
      assertEquals("Checking for chain of 0", 1.0, SimulationTask.getComboMultiplier(0), 0.1);
      assertEquals("Checking for chain of 1", 1.0, SimulationTask.getComboMultiplier(1), 0.1);
      for (int i = 2; i <= 4; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 1.1, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 5; i <= 9; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 1.15, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 10; i <= 24; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 1.2, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 25; i <= 49; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 1.3, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 50; i <= 74; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 1.4, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 75; i <= 99; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 1.5, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 100; i <= 199; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 2, SimulationTask.getComboMultiplier(i), 0.001);
      }
      for (int i = 200; i <= 210; i++) {
         assertEquals(String.format("Checking for chain of %s", i), 2.5, SimulationTask.getComboMultiplier(i), 0.001);
      }
   }
   
}

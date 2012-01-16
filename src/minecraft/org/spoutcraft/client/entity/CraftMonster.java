/*
 * This file is part of Spoutcraft (http://spout.org).
 * 
 * Spoutcraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spoutcraft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.spoutcraft.client.entity;

import net.minecraft.src.EntityMob;

import org.spoutcraft.spoutcraftapi.entity.Monster;

public class CraftMonster extends CraftCreature implements Monster {

	public CraftMonster(EntityMob entity) {
		super(entity);
	}

	@Override
	public String toString() {
		return "CraftMonster";
	}

	public EntityMob getEntityMonster() {
		return (EntityMob) handle;
	}
}

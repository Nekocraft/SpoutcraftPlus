/*
 * This file is part of Spoutcraft.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spoutcraft is licensed under the GNU Lesser General Public License.
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
package org.spoutcraft.client.gui.settings.controls;

import net.minecraft.client.Minecraft;

import org.spoutcraft.client.config.Configuration;

public class ServerLightButton extends AutomatedCheckBox {
	public ServerLightButton() {
		super("光照计算");
		setChecked(Configuration.isClientLight());
		setTooltip("在多服务器人重新计算光照。\n" +
				"\n" +
				"禁用重新计算速度更快，但可能会导致在奇数\n" +
				"光图案或光孔.");
	}

	@Override
	public void onButtonClick() {
		Configuration.setClientLight(!Configuration.isClientLight());
		Configuration.write();
	}

	@Override
	public String getTooltip() {
		if (Minecraft.theMinecraft.theWorld == null || Minecraft.theMinecraft.theWorld.isRemote) {
			return super.getTooltip();
		}

		return "在单人游戏中有没有效果";
	}
}

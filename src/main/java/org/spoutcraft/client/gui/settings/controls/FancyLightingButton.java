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

import java.util.UUID;

import net.minecraft.client.Minecraft;

import org.spoutcraft.client.config.Configuration;

public class FancyLightingButton extends AutomatedCheckBox {
	UUID fancyGraphics;
	public FancyLightingButton(UUID fancyGraphics) {
		super("灯渲染");
		this.fancyGraphics = fancyGraphics;
		setChecked(Configuration.isFancyLight());
		setTooltip("灯渲染\n" +
				"快 - 低质量，更快\n" +
				"质量 - 质量更高，速度较慢\n" +
				"质量适用于更先进的照明块。\n" +
				"拥有柔和的灯光效果最佳.");
	}

	@Override
	public void onButtonClick() {
		Configuration.setFancyLight(!Configuration.isFancyLight());
		Configuration.write();
		((FancyGraphicsButton)getScreen().getWidget(fancyGraphics)).custom = true;

		if (Minecraft.theMinecraft.theWorld != null) {
			Minecraft.theMinecraft.renderGlobal.updateAllRenderers();
		}
	}
}

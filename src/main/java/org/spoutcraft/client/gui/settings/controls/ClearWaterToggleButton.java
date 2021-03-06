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
import net.minecraft.src.Block;

import org.spoutcraft.api.gui.GenericCheckBox;
import org.spoutcraft.client.SpoutClient;
import org.spoutcraft.client.config.Configuration;

public class ClearWaterToggleButton extends GenericCheckBox {
	public ClearWaterToggleButton() {
		super("清澈的水");
		setChecked(Configuration.isClearWater());
		setEnabled(SpoutClient.getInstance().isClearWaterCheat());
		setTooltip("清澈的水\n关闭 - （默认）标准的水景\n - 清澈的水可以看到更深的水，不再掩盖视力\n清除水需要大量资源！");
	}

	@Override
	public String getTooltip() {
		if (!isEnabled()) {
			return "此选项在服务器不被允许，它被视为作弊.";
		}

		return super.getTooltip();
	}

	@Override
	public void onButtonClick() {
		Configuration.setClearWater(!Configuration.isClearWater());
		Configuration.write();
		Block.waterStill.setLightOpacity(isChecked() ? 1 : 3);
		Block.waterMoving.setLightOpacity(isChecked() ? 1 : 3);

		if (Minecraft.theMinecraft.theWorld != null) {
			Minecraft.theMinecraft.renderGlobal.updateAllRenderers();
		}
	}
}

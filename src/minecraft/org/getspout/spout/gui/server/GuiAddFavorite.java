package org.getspout.spout.gui.server;

import org.getspout.spout.client.SpoutClient;
import org.spoutcraft.spoutcraftapi.Spoutcraft;
import org.spoutcraft.spoutcraftapi.addon.Addon;
import org.spoutcraft.spoutcraftapi.event.screen.TextFieldChangeEvent;
import org.spoutcraft.spoutcraftapi.gui.Button;
import org.spoutcraft.spoutcraftapi.gui.GenericButton;
import org.spoutcraft.spoutcraftapi.gui.GenericLabel;
import org.spoutcraft.spoutcraftapi.gui.GenericTextField;
import org.spoutcraft.spoutcraftapi.gui.Keyboard;
import org.spoutcraft.spoutcraftapi.gui.Label;
import org.spoutcraft.spoutcraftapi.gui.TextField;

import net.minecraft.src.GuiScreen;

public class GuiAddFavorite extends GuiScreen {

	private TextField textIp, textTitle;
	private ServerItem item = null;
	private ServerItem toUpdate = null;
	private Label labelTitle, labelIp;
	private boolean update = false;
	private Button buttonDone, buttonCancel, buttonClear;
	private GuiScreen parent;
	
	public GuiAddFavorite(String server, GuiScreen parent) {
		String splt[] = server.split(":");
		int port = 25565;
		if(splt.length > 0) {
			server = splt[0];
			if(splt.length > 1) {
				try {
					port = Integer.valueOf(splt[1]);
				} catch (NumberFormatException e){
					port = 25565;
				}
			}
		} else {
			server = "";
		}
		item = new ServerItem("", server, port, -1);
		update = false;
		this.parent = parent;
	}
	
	public GuiAddFavorite(ServerItem toUpdate, GuiScreen parent) {
		item = toUpdate.clone();
		this.toUpdate = toUpdate;
		update = true;
		this.parent = parent;
	}
	
	@Override
	public void initGui() {
		Addon spoutcraft = Spoutcraft.getAddonManager().getAddon("Spoutcraft");
		int top = height / 2 - 101/2;
		int left = width / 2 - 250 / 2;
		
		updateItem();
		
		labelTitle = new GenericLabel("Server Name");
		labelTitle.setHeight(11).setWidth(250).setX(left).setY(top);
		getScreen().attachWidget(spoutcraft, labelTitle);
		top+=13;
		
		textTitle = new TabField();
		textTitle.setMaximumCharacters(0).setWidth(250).setHeight(20).setX(left).setY(top);
		textTitle.setText(item.getTitle());
		getScreen().attachWidget(spoutcraft, textTitle);
		top+=25;
		
		labelIp = new GenericLabel("Server Address");
		labelIp.setHeight(11).setWidth(250).setX(left).setY(top);
		getScreen().attachWidget(spoutcraft, labelIp);
		top+=13;
		
		textIp = new TabField();
		textIp.setMaximumCharacters(0);
		textIp.setWidth(250);
		textIp.setHeight(20);
		textIp.setX(left).setY(top);
		getScreen().attachWidget(spoutcraft, textIp);
		textIp.setText(item.getIp() + (item.getPort()!=25565?":"+item.getPort():""));
		top+=25;
		
		buttonClear = new GenericButton("Clear");
		buttonClear.setWidth(100).setHeight(20).setX(textIp.getX()).setY(top);
		getScreen().attachWidget(spoutcraft, buttonClear);
		
		buttonDone = new GenericButton("Done");
		buttonDone.setWidth(200).setHeight(20).setX(width - 205).setY(height - 25);
		getScreen().attachWidget(spoutcraft, buttonDone);
		
		buttonCancel = new GenericButton("Cancel");
		buttonCancel.setWidth(200).setHeight(20).setX(width - 205 - 205).setY(height - 25);
		getScreen().attachWidget(spoutcraft, buttonCancel);
		
		updateButtons();
	}
	
	private class TabField extends GenericTextField {
		
		@Override
		public void onTextFieldChange(TextFieldChangeEvent event) {
			updateButtons();
		}

		@Override
		public boolean onKeyPressed(Keyboard key) {
			if (key == Keyboard.KEY_TAB) {
				if (textIp.isFocus()) {
					textIp.setFocus(false);
					textTitle.setFocus(true);
				} else {
					textTitle.setFocus(false);
					textIp.setFocus(true);
				}
				return true;
			}
			return false;
		}
		
	}
	
	@Override
	protected void buttonClicked(Button btn) {
		if(btn.equals(buttonDone)) {
			updateItem();
			if(item.getTitle().isEmpty() || item.getIp().isEmpty()) {
				SpoutClient.getHandle().displayGuiScreen(parent);
				return;
			}
			if(update) {
				//Update original item
				toUpdate.update(item);
			} else {				
				SpoutClient.getInstance().getServerManager().getFavorites().addServer(item);
			}
			SpoutClient.getInstance().getServerManager().getFavorites().save();
			SpoutClient.getHandle().displayGuiScreen(parent);
		}
		if(btn.equals(buttonCancel)) {
			SpoutClient.getHandle().displayGuiScreen(parent);
		}
		if(btn == buttonClear) {
			textTitle.setText("");
			textIp.setText("");
			item.setTitle("");
			item.setIp("");
			item.setPort(25565);
			updateButtons();
		}
	}
	
	protected void updateButtons() {
		buttonDone.setEnabled(!textIp.getText().isEmpty() && !textTitle.getText().isEmpty());
	}
	
	private void updateItem() {
		if(textTitle != null) {
			item.setTitle(textTitle.getText());
		}
		if(textIp != null) {
			String split[] = textIp.getText().split(":");
			item.setIp(split[0]);
			try {
				item.setPort(Integer.valueOf(split[1]));
			} catch(Exception e){
				//Handles both InvalidNumber and OutOfRange exceptions, yay
				item.setPort(25565);
			}
		}
		
	}
	
	@Override
	public void drawScreen(int a, int b, float f) {
		drawDefaultBackground();
	}
}

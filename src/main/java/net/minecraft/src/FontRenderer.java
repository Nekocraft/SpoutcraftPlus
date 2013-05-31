package net.minecraft.src;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.Bidi;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class FontRenderer {

	/** Array of width of all the characters in default.png */
	private float[] charWidth = new float[256];

	/** the height in pixels of default text */
	public int FONT_HEIGHT = 9;
	public Random fontRandom = new Random();

	/**
	 * Array of the start/end column (in upper/lower nibble) for every glyph in the /font directory.
	 */
	private byte[] glyphWidth = new byte[65536];

	/**
	 * Array of RGB triplets defining the 16 standard chat colors followed by 16 darker version of the same colors for drop
	 * shadows.
	 */
	private int[] colorCode = new int[32];
	private final String fontTextureName;

	/** The RenderEngine used to load and setup glyph textures. */
	private final RenderEngine renderEngine;

	/** Current X coordinate at which to draw the next character. */
	private float posX;

	/** Current Y coordinate at which to draw the next character. */
	private float posY;

	/**
	 * If true, strings should be rendered with Unicode fonts instead of the default.png font
	 */
	private boolean unicodeFlag;

	/**
	 * If true, the Unicode Bidirectional Algorithm should be run before rendering any string.
	 */
	private boolean bidiFlag;

	/** Used to specify new red value for the current color. */
	private float red;

	/** Used to specify new blue value for the current color. */
	private float blue;

	/** Used to specify new green value for the current color. */
	private float green;

	/** Used to speify new alpha value for the current color. */
	private float alpha;

	/** Text color of the currently rendering string. */
	private int textColor;

	/** Set if the "k" style (random) is active in currently rendering string */
	private boolean randomStyle = false;

	/** Set if the "l" style (bold) is active in currently rendering string */
	private boolean boldStyle = false;

	/** Set if the "o" style (italic) is active in currently rendering string */
	private boolean italicStyle = false;

	/**
	 * Set if the "n" style (underlined) is active in currently rendering string
	 */
	private boolean underlineStyle = false;

	/**
	 * Set if the "m" style (strikethrough) is active in currently rendering string
	 */
	private boolean strikethroughStyle = false;
	public GameSettings gameSettings;
	public boolean enabled = true;

	FontRenderer() {
		this.renderEngine = null;
		this.fontTextureName = null;
	}

	public FontRenderer(GameSettings par1GameSettings, String par2Str, RenderEngine par3RenderEngine, boolean par4) {
		this.gameSettings = par1GameSettings;
		this.fontTextureName = par2Str;
		this.renderEngine = par3RenderEngine;
		this.unicodeFlag = par4;
		this.readFontData();
		par3RenderEngine.bindTexture(par2Str);

		for (int var5 = 0; var5 < 32; ++var5) {
			int var6 = (var5 >> 3 & 1) * 85;
			int var7 = (var5 >> 2 & 1) * 170 + var6;
			int var8 = (var5 >> 1 & 1) * 170 + var6;
			int var9 = (var5 >> 0 & 1) * 170 + var6;

			if (var5 == 6) {
				var7 += 85;
			}

			if (par1GameSettings.anaglyph) {
				int var10 = (var7 * 30 + var8 * 59 + var9 * 11) / 100;
				int var11 = (var7 * 30 + var8 * 70) / 100;
				int var12 = (var7 * 30 + var9 * 70) / 100;
				var7 = var10;
				var8 = var11;
				var9 = var12;
			}

			if (var5 >= 16) {
				var7 /= 4;
				var8 /= 4;
				var9 /= 4;
			}

			this.colorCode[var5] = (var7 & 255) << 16 | (var8 & 255) << 8 | var9 & 255;
		}
	}

	public void readFontData() {
		this.readGlyphSizes();
		this.readFontTexture(this.fontTextureName);
	}

	private void readFontTexture(String par1Str) {
		BufferedImage var2;

		try {
			par1Str = fixHdFontName(par1Str);
			var2 = ImageIO.read(getFontTexturePack().getResourceAsStream(par1Str));
		} catch (IOException var19) {
			throw new RuntimeException(var19);
		}

		int var3 = var2.getWidth();
		int var4 = var2.getHeight();
		int var5 = var3 / 16;
		int var6 = var4 / 16;
		float var7 = (float)var3 / 128.0F;
		int[] var8 = new int[var3 * var4];
		var2.getRGB(0, 0, var3, var4, var8, 0, var3);
		int var9 = 0;

		while (var9 < 256) {
			int var10 = var9 % 16;
			int var11 = var9 / 16;
			boolean var12 = false;
			int var20 = var5 - 1;

			while (true) {
				if (var20 >= 0) {
					int var13 = var10 * var5 + var20;
					boolean var14 = true;

					for (int var15 = 0; var15 < var6 && var14; ++var15) {
						int var16 = (var11 * var6 + var15) * var3;
						int var17 = var8[var13 + var16];
						int var18 = var17 >> 24 & 255;

						if (var18 > 16) {
							var14 = false;
						}
					}

					if (var14) {
						--var20;
						continue;
					}
				}

				if (var9 == 65) {
					var9 = var9;
				}

				if (var9 == 32) {
					if (var5 <= 8) {
						var20 = (int)(2.0F * var7);
					} else {
						var20 = (int)(1.5F * var7);
					}
				}

				this.charWidth[var9] = (float)(var20 + 1) / var7 + 1.0F;
				++var9;
				break;
			}
		}

		this.readCustomCharWidths();
	}

	private void readGlyphSizes() {
		try {
			InputStream var1 = getFontTexturePack().getResourceAsStream("/font/glyph_sizes.bin");
			var1.read(this.glyphWidth);
		} catch (IOException var2) {
			throw new RuntimeException(var2);
		}
	}

	/**
	 * Pick how to render a single character and return the width used.
	 */
	private float renderCharAtPos(int par1, char par2, boolean par3) {
		return par2 == 32 ? this.charWidth[par2] : (par1 > 0 && !this.unicodeFlag ? this.renderDefaultChar(par1 + 32, par3) : this.renderUnicodeChar(par2, par3));
	}

	/**
	 * Render a single character with the default.png font at current (posX,posY) location...
	 */
	private float renderDefaultChar(int par1, boolean par2) {
		float var3 = (float)(par1 % 16 * 8);
		float var4 = (float)(par1 / 16 * 8);
		float var5 = par2 ? 1.0F : 0.0F;
		this.renderEngine.bindTexture(this.fontTextureName);
		float var6 = this.charWidth[par1] - 0.01F;
		GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
		GL11.glTexCoord2f(var3 / 128.0F, var4 / 128.0F);
		GL11.glVertex3f(this.posX + var5, this.posY, 0.0F);
		GL11.glTexCoord2f(var3 / 128.0F, (var4 + 7.99F) / 128.0F);
		GL11.glVertex3f(this.posX - var5, this.posY + 7.99F, 0.0F);
		GL11.glTexCoord2f((var3 + var6) / 128.0F, var4 / 128.0F);
		GL11.glVertex3f(this.posX + var6 + var5, this.posY, 0.0F);
		GL11.glTexCoord2f((var3 + var6) / 128.0F, (var4 + 7.99F) / 128.0F);
		GL11.glVertex3f(this.posX + var6 - var5, this.posY + 7.99F, 0.0F);
		GL11.glEnd();
		return this.charWidth[par1];
	}

	/**
	 * Load one of the /font/glyph_XX.png into a new GL texture and store the texture ID in glyphTextureName array.
	 */
	private void loadGlyphTexture(int par1) {
		String var2 = String.format("/font/glyph_%02X.png", new Object[] {Integer.valueOf(par1)});
		this.renderEngine.bindTexture(var2);
	}

	/**
	 * Render a single Unicode character at current (posX,posY) location using one of the /font/glyph_XX.png files...
	 */
	private float renderUnicodeChar(char par1, boolean par2) {
		if (this.glyphWidth[par1] == 0) {
			return 0.0F;
		} else {
			int var3 = par1 / 256;
			this.loadGlyphTexture(var3);
			int var4 = this.glyphWidth[par1] >>> 4;
			int var5 = this.glyphWidth[par1] & 15;
			float var6 = (float)var4;
			float var7 = (float)(var5 + 1);
			float var8 = (float)(par1 % 16 * 16) + var6;
			float var9 = (float)((par1 & 255) / 16 * 16);
			float var10 = var7 - var6 - 0.02F;
			float var11 = par2 ? 1.0F : 0.0F;
			GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
			GL11.glTexCoord2f(var8 / 256.0F, var9 / 256.0F);
			GL11.glVertex3f(this.posX + var11, this.posY, 0.0F);
			GL11.glTexCoord2f(var8 / 256.0F, (var9 + 15.98F) / 256.0F);
			GL11.glVertex3f(this.posX - var11, this.posY + 7.99F, 0.0F);
			GL11.glTexCoord2f((var8 + var10) / 256.0F, var9 / 256.0F);
			GL11.glVertex3f(this.posX + var10 / 2.0F + var11, this.posY, 0.0F);
			GL11.glTexCoord2f((var8 + var10) / 256.0F, (var9 + 15.98F) / 256.0F);
			GL11.glVertex3f(this.posX + var10 / 2.0F - var11, this.posY + 7.99F, 0.0F);
			GL11.glEnd();
			return (var7 - var6) / 2.0F + 1.0F;
		}
	}

	/**
	 * Draws the specified string with a shadow.
	 */
	public int drawStringWithShadow(String par1Str, int par2, int par3, int par4) {
		return this.drawString(par1Str, par2, par3, par4, true);
	}

	/**
	 * Draws the specified string.
	 */
	public int drawString(String par1Str, int par2, int par3, int par4) {
		return !this.enabled ? 0 : this.drawString(par1Str, par2, par3, par4, false);
	}

	/**
	 * Draws the specified string. Args: string, x, y, color, dropShadow
	 */
	public int drawString(String par1Str, int par2, int par3, int par4, boolean par5) {
		this.resetStyles();

		if (this.bidiFlag) {
			par1Str = this.bidiReorder(par1Str);
		}

		int var6;

		if (par5) {
			var6 = this.renderString(par1Str, par2 + 1, par3 + 1, par4, true);
			var6 = Math.max(var6, this.renderString(par1Str, par2, par3, par4, false));
		} else {
			var6 = this.renderString(par1Str, par2, par3, par4, false);
		}

		return var6;
	}

	/**
	 * Apply Unicode Bidirectional Algorithm to string and return a new possibly reordered string for visual rendering.
	 */
	private String bidiReorder(String par1Str) {
		if (par1Str != null && Bidi.requiresBidi(par1Str.toCharArray(), 0, par1Str.length())) {
			Bidi var2 = new Bidi(par1Str, -2);
			byte[] var3 = new byte[var2.getRunCount()];
			String[] var4 = new String[var3.length];
			int var5;

			for (int var6 = 0; var6 < var3.length; ++var6) {
				int var7 = var2.getRunStart(var6);
				var5 = var2.getRunLimit(var6);
				int var8 = var2.getRunLevel(var6);
				String var9 = par1Str.substring(var7, var5);
				var3[var6] = (byte)var8;
				var4[var6] = var9;
			}

			String[] var13 = (String[])((String[])var4.clone());
			Bidi.reorderVisually(var3, 0, var4, 0, var3.length);
			StringBuilder var14 = new StringBuilder();
			var5 = 0;

			while (var5 < var4.length) {
				byte var12 = var3[var5];
				int var11 = 0;

				while (true) {
					if (var11 < var13.length) {
						if (!var13[var11].equals(var4[var5])) {
							++var11;
							continue;
						}

						var12 = var3[var11];
					}

					if ((var12 & 1) == 0) {
						var14.append(var4[var5]);
					} else {
						for (var11 = var4[var5].length() - 1; var11 >= 0; --var11) {
							char var10 = var4[var5].charAt(var11);

							if (var10 == 40) {
								var10 = 41;
							} else if (var10 == 41) {
								var10 = 40;
							}

							var14.append(var10);
						}
					}

					++var5;
					break;
				}
			}

			return var14.toString();
		} else {
			return par1Str;
		}
	}

	/**
	 * Reset all style flag fields in the class to false; called at the start of string rendering
	 */
	private void resetStyles() {
		this.randomStyle = false;
		this.boldStyle = false;
		this.italicStyle = false;
		this.underlineStyle = false;
		this.strikethroughStyle = false;
	}

	/**
	 * Render a single line string at the current (posX,posY) and update posX
	 */
	private void renderStringAtPos(String par1Str, boolean par2) {
		for (int var3 = 0; var3 < par1Str.length(); ++var3) {
			char var4 = par1Str.charAt(var3);
			int var5;
			int var6;

			if (var4 == 167 && var3 + 1 < par1Str.length()) {
				var5 = "0123456789abcdefklmnor".indexOf(par1Str.toLowerCase().charAt(var3 + 1));

				if (var5 < 16) {
					this.randomStyle = false;
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;

					if (var5 < 0 || var5 > 15) {
						var5 = 15;
					}

					if (par2) {
						var5 += 16;
					}

					var6 = this.colorCode[var5];
					this.textColor = var6;
					GL11.glColor4f((float)(var6 >> 16) / 255.0F, (float)(var6 >> 8 & 255) / 255.0F, (float)(var6 & 255) / 255.0F, this.alpha);
				} else if (var5 == 16) {
					this.randomStyle = true;
				} else if (var5 == 17) {
					this.boldStyle = true;
				} else if (var5 == 18) {
					this.strikethroughStyle = true;
				} else if (var5 == 19) {
					this.underlineStyle = true;
				} else if (var5 == 20) {
					this.italicStyle = true;
				} else if (var5 == 21) {
					this.randomStyle = false;
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;
					GL11.glColor4f(this.red, this.blue, this.green, this.alpha);
				}

				++var3;
			} else {
				var5 = ChatAllowedCharacters.allowedCharacters.indexOf(var4);

				if (this.randomStyle && var5 > 0) {
					do {
						var6 = this.fontRandom.nextInt(ChatAllowedCharacters.allowedCharacters.length());
					} while ((int)this.charWidth[var5 + 32] != (int)this.charWidth[var6 + 32]);

					var5 = var6;
				}

				float var7 = this.unicodeFlag ? 0.5F : 1.0F;
				boolean var8 = (var5 <= 0 || this.unicodeFlag) && par2;

				if (var8) {
					this.posX -= var7;
					this.posY -= var7;
				}

				float var9 = this.renderCharAtPos(var5, var4, this.italicStyle);

				if (var8) {
					this.posX += var7;
					this.posY += var7;
				}

				if (this.boldStyle) {
					this.posX += var7;

					if (var8) {
						this.posX -= var7;
						this.posY -= var7;
					}

					this.renderCharAtPos(var5, var4, this.italicStyle);
					this.posX -= var7;

					if (var8) {
						this.posX += var7;
						this.posY += var7;
					}

					++var9;
				}

				Tessellator var10;

				if (this.strikethroughStyle) {
					var10 = Tessellator.instance;
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					var10.startDrawingQuads();
					var10.addVertex((double)this.posX, (double)(this.posY + (float)(this.FONT_HEIGHT / 2)), 0.0D);
					var10.addVertex((double)(this.posX + var9), (double)(this.posY + (float)(this.FONT_HEIGHT / 2)), 0.0D);
					var10.addVertex((double)(this.posX + var9), (double)(this.posY + (float)(this.FONT_HEIGHT / 2) - 1.0F), 0.0D);
					var10.addVertex((double)this.posX, (double)(this.posY + (float)(this.FONT_HEIGHT / 2) - 1.0F), 0.0D);
					var10.draw();
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}

				if (this.underlineStyle) {
					var10 = Tessellator.instance;
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					var10.startDrawingQuads();
					int var11 = this.underlineStyle ? -1 : 0;
					var10.addVertex((double)(this.posX + (float)var11), (double)(this.posY + (float)this.FONT_HEIGHT), 0.0D);
					var10.addVertex((double)(this.posX + var9), (double)(this.posY + (float)this.FONT_HEIGHT), 0.0D);
					var10.addVertex((double)(this.posX + var9), (double)(this.posY + (float)this.FONT_HEIGHT - 1.0F), 0.0D);
					var10.addVertex((double)(this.posX + (float)var11), (double)(this.posY + (float)this.FONT_HEIGHT - 1.0F), 0.0D);
					var10.draw();
					GL11.glEnable(GL11.GL_TEXTURE_2D);
				}

				this.posX += var9;
			}
		}
	}

	/**
	 * Render string either left or right aligned depending on bidiFlag
	 */
	private int renderStringAligned(String par1Str, int par2, int par3, int par4, int par5, boolean par6) {
		if (this.bidiFlag) {
			par1Str = this.bidiReorder(par1Str);
			int var7 = this.getStringWidth(par1Str);
			par2 = par2 + par4 - var7;
		}

		return this.renderString(par1Str, par2, par3, par5, par6);
	}

	/**
	 * Render single line string by setting GL color, current (posX,posY), and calling renderStringAtPos()
	 */
	private int renderString(String par1Str, int par2, int par3, int par4, boolean par5) {
		if (par1Str == null) {
			return 0;
		} else {
			if ((par4 & -67108864) == 0) {
				par4 |= -16777216;
			}

			if (par5) {
				par4 = (par4 & 16579836) >> 2 | par4 & -16777216;
			}

			this.red = (float)(par4 >> 16 & 255) / 255.0F;
			this.blue = (float)(par4 >> 8 & 255) / 255.0F;
			this.green = (float)(par4 & 255) / 255.0F;
			this.alpha = (float)(par4 >> 24 & 255) / 255.0F;
			GL11.glColor4f(this.red, this.blue, this.green, this.alpha);
			this.posX = (float)par2;
			this.posY = (float)par3;
			this.renderStringAtPos(par1Str, par5);
			return (int)this.posX;
		}
	}

	/**
	 * Returns the width of this string. Equivalent of FontMetrics.stringWidth(String s).
	 */
	public int getStringWidth(String par1Str) {
		if (par1Str == null) {
			return 0;
		} else {
			float var2 = 0.0F;
			boolean var3 = false;

			for (int var4 = 0; var4 < par1Str.length(); ++var4) {
				char var5 = par1Str.charAt(var4);
				float var6 = this.getCharWidthFloat(var5);

				if (var6 < 0.0F && var4 < par1Str.length() - 1) {
					++var4;
					var5 = par1Str.charAt(var4);

					if (var5 != 108 && var5 != 76) {
						if (var5 == 114 || var5 == 82) {
							var3 = false;
						}
					} else {
						var3 = true;
					}

					var6 = 0.0F;
				}

				var2 += var6;

				if (var3) {
					++var2;
				}
			}

			return (int)var2;
		}
	}

	/**
	 * Returns the width of this character as rendered.
	 */
	public int getCharWidth(char par1) {
		return Math.round(this.getCharWidthFloat(par1));
	}

	private float getCharWidthFloat(char var1) {
		if (var1 == 167) {
			return -1.0F;
		} else if (var1 == 32) {
			return this.charWidth[32];
		} else {
			int var2 = ChatAllowedCharacters.allowedCharacters.indexOf(var1);

			if (var2 >= 0 && !this.unicodeFlag) {
				return this.charWidth[var2 + 32];
			} else if (this.glyphWidth[var1] != 0) {
				int var3 = this.glyphWidth[var1] >>> 4;
				int var4 = this.glyphWidth[var1] & 15;

				if (var4 > 7) {
					var4 = 15;
					var3 = 0;
				}

				++var4;
				return (float)((var4 - var3) / 2 + 1);
			} else {
				return 0.0F;
			}
		}
	}

	/**
	 * Trims a string to fit a specified Width.
	 */
	public String trimStringToWidth(String par1Str, int par2) {
		return this.trimStringToWidth(par1Str, par2, false);
	}

	/**
	 * Trims a string to a specified width, and will reverse it if par3 is set.
	 */
	public String trimStringToWidth(String par1Str, int par2, boolean par3) {
		StringBuilder var4 = new StringBuilder();
		float var5 = 0.0F;
		int var6 = par3 ? par1Str.length() - 1 : 0;
		int var7 = par3 ? -1 : 1;
		boolean var8 = false;
		boolean var9 = false;

		for (int var10 = var6; var10 >= 0 && var10 < par1Str.length() && var5 < (float)par2; var10 += var7) {
			char var11 = par1Str.charAt(var10);
			float var12 = this.getCharWidthFloat(var11);

			if (var8) {
				var8 = false;

				if (var11 != 108 && var11 != 76) {
					if (var11 == 114 || var11 == 82) {
						var9 = false;
					}
				} else {
					var9 = true;
				}
			} else if (var12 < 0.0F) {
				var8 = true;
			} else {
				var5 += var12;

				if (var9) {
					++var5;
				}
			}

			if (var5 > (float)par2) {
				break;
			}

			if (par3) {
				var4.insert(0, var11);
			} else {
				var4.append(var11);
			}
		}

		return var4.toString();
	}

	/**
	 * Remove all newline characters from the end of the string
	 */
	private String trimStringNewline(String par1Str) {
		while (par1Str != null && par1Str.endsWith("\n")) {
			par1Str = par1Str.substring(0, par1Str.length() - 1);
		}

		return par1Str;
	}

	/**
	 * Splits and draws a String with wordwrap (maximum length is parameter k)
	 */
	public void drawSplitString(String par1Str, int par2, int par3, int par4, int par5) {
		this.resetStyles();
		this.textColor = par5;
		par1Str = this.trimStringNewline(par1Str);
		this.renderSplitString(par1Str, par2, par3, par4, false);
	}

	/**
	 * Perform actual work of rendering a multi-line string with wordwrap and with darker drop shadow color if flag is set
	 */
	private void renderSplitString(String par1Str, int par2, int par3, int par4, boolean par5) {
		List var6 = this.listFormattedStringToWidth(par1Str, par4);

		for (Iterator var7 = var6.iterator(); var7.hasNext(); par3 += this.FONT_HEIGHT) {
			String var8 = (String)var7.next();
			this.renderStringAligned(var8, par2, par3, par4, this.textColor, par5);
		}
	}

	/**
	 * Returns the width of the wordwrapped String (maximum length is parameter k)
	 */
	public int splitStringWidth(String par1Str, int par2) {
		return this.FONT_HEIGHT * this.listFormattedStringToWidth(par1Str, par2).size();
	}

	/**
	 * Set unicodeFlag controlling whether strings should be rendered with Unicode fonts instead of the default.png font.
	 */
	public void setUnicodeFlag(boolean par1) {
		this.unicodeFlag = par1;
	}

	/**
	 * Get unicodeFlag controlling whether strings should be rendered with Unicode fonts instead of the default.png font.
	 */
	public boolean getUnicodeFlag() {
		return this.unicodeFlag;
	}

	/**
	 * Set bidiFlag to control if the Unicode Bidirectional Algorithm should be run before rendering any string.
	 */
	public void setBidiFlag(boolean par1) {
		this.bidiFlag = par1;
	}

	/**
	 * Breaks a string into a list of pieces that will fit a specified width.
	 */
	public List listFormattedStringToWidth(String par1Str, int par2) {
		return Arrays.asList(this.wrapFormattedStringToWidth(par1Str, par2).split("\n"));
	}

	/**
	 * Inserts newline and formatting into a string to wrap it within the specified width.
	 */
	String wrapFormattedStringToWidth(String par1Str, int par2) {
		int var3 = this.sizeStringToWidth(par1Str, par2);

		if (par1Str.length() <= var3) {
			return par1Str;
		} else {
			String var4 = par1Str.substring(0, var3);
			char var5 = par1Str.charAt(var3);
			boolean var6 = var5 == 32 || var5 == 10;
			String var7 = getFormatFromString(var4) + par1Str.substring(var3 + (var6 ? 1 : 0));
			return var4 + "\n" + this.wrapFormattedStringToWidth(var7, par2);
		}
	}

	/**
	 * Determines how many characters from the string will fit into the specified width.
	 */
	private int sizeStringToWidth(String par1Str, int par2) {
		int var3 = par1Str.length();
		float var4 = 0.0F;
		int var5 = 0;
		int var6 = -1;

		for (boolean var7 = false; var5 < var3; ++var5) {
			char var8 = par1Str.charAt(var5);

			switch (var8) {
				case 10:
					--var5;
					break;

				case 167:
					if (var5 < var3 - 1) {
						++var5;
						char var9 = par1Str.charAt(var5);

						if (var9 != 108 && var9 != 76) {
							if (var9 == 114 || var9 == 82 || isFormatColor(var9)) {
								var7 = false;
							}
						} else {
							var7 = true;
						}
					}

					break;

				case 32:
					var6 = var5;

				default:
					var4 += this.getCharWidthFloat(var8);

					if (var7) {
						++var4;
					}
			}

			if (var8 == 10) {
				++var5;
				var6 = var5;
				break;
			}

			if (var4 > (float)par2) {
				break;
			}
		}

		return var5 != var3 && var6 != -1 && var6 < var5 ? var6 : var5;
	}

	/**
	 * Checks if the char code is a hexadecimal character, used to set colour.
	 */
	private static boolean isFormatColor(char par0) {
		return par0 >= 48 && par0 <= 57 || par0 >= 97 && par0 <= 102 || par0 >= 65 && par0 <= 70;
	}

	/**
	 * Checks if the char code is O-K...lLrRk-o... used to set special formatting.
	 */
	private static boolean isFormatSpecial(char par0) {
		return par0 >= 107 && par0 <= 111 || par0 >= 75 && par0 <= 79 || par0 == 114 || par0 == 82;
	}

	/**
	 * Digests a string for nonprinting formatting characters then returns a string containing only that formatting.
	 */
	private static String getFormatFromString(String par0Str) {
		String var1 = "";
		int var2 = -1;
		int var3 = par0Str.length();

		while ((var2 = par0Str.indexOf(167, var2 + 1)) != -1) {
			if (var2 < var3 - 1) {
				char var4 = par0Str.charAt(var2 + 1);

				if (isFormatColor(var4)) {
					var1 = "\u00a7" + var4;
				} else if (isFormatSpecial(var4)) {
					var1 = var1 + "\u00a7" + var4;
				}
			}
		}

		return var1;
	}

	/**
	 * Get bidiFlag that controls if the Unicode Bidirectional Algorithm should be run before rendering any string
	 */
	public boolean getBidiFlag() {
		return this.bidiFlag;
	}

	private void readCustomCharWidths() {
		String var1 = fixHdFontName(this.fontTextureName);
		String var2 = ".png";

		if (var1.endsWith(var2)) {
			String var3 = var1.substring(0, var1.length() - var2.length()) + ".properties";

			try {
				InputStream var4 = getFontTexturePack().getResourceAsStream(var3);

				if (var4 == null) {
					return;
				}

				Properties var5 = new Properties();
				var5.load(var4);
				Set var6 = var5.keySet();
				Iterator var7 = var6.iterator();

				while (var7.hasNext()) {
					String var8 = (String)var7.next();
					String var9 = "width.";

					if (var8.startsWith(var9)) {
						String var10 = var8.substring(var9.length());
						int var11 = parseInt(var10, -1);

						if (var11 >= 0 && var11 < this.charWidth.length) {
							String var12 = var5.getProperty(var8);
							float var13 = parseFloat(var12, -1.0F);

							if (var13 >= 0.0F) {
								this.charWidth[var11] = var13;
							}
						}
					}
				}
			} catch (FileNotFoundException var14) {
				;
			} catch (IOException var15) {
				var15.printStackTrace();
			}
		}
	}

	private static String fixHdFontName(String var0) {
		if (var0 == null) {
			return var0;
		} else {
			String var1 = ".png";

			if (!var0.endsWith(var1)) {
				return var0;
			} else {
				String var2 = var0.substring(0, var0.length() - var1.length());
				String var3 = var2 + "_hd" + var1;
				return getFontTexturePack().func_98138_b(var3, true) ? var3 : var0;
			}
		}
	}

	private static ITexturePack getFontTexturePack() {
		return Minecraft.getMinecraft().texturePackList.getSelectedTexturePack();
	}

	public static int parseInt(String var0, int var1) {
		try {
			return var0 == null ? var1 : Integer.parseInt(var0);
		} catch (NumberFormatException var3) {
			return var1;
		}
	}

	public static float parseFloat(String var0, float var1) {
		try {
			return var0 == null ? var1 : Float.parseFloat(var0);
		} catch (NumberFormatException var3) {
			return var1;
		}
	}
}

package appeng.client.gui.implementations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import appeng.api.AEApi;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternMulti;
import appeng.core.AEConfig;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternMultiSet;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.Reflected;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;

public class GuiPatternMulti extends AEBaseGui {

    private GuiTextField amountToSet;
    private GuiTabButton originalGuiBtn;
    private GuiImgButton symbolSwitch;
    private GuiButton set;

    private GuiButton plus1;
    private GuiButton plus10;
    private GuiButton plus100;
    private GuiButton plus1000;
    private GuiButton minus1;
    private GuiButton minus10;
    private GuiButton minus100;
    private GuiButton minus1000;

    private GuiBridge originalGui;

    private static final int DEFAULT_VALUE = 0;

    @Reflected
    public GuiPatternMulti(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerPatternMulti(inventoryPlayer, te));
        GuiContainer gui = (GuiContainer) Minecraft.getMinecraft().currentScreen;
    }

    @Override
    public void initGui() {
        super.initGui();

        final int a = AEConfig.instance.craftItemsByStackAmounts(0);
        final int b = AEConfig.instance.craftItemsByStackAmounts(1);
        final int c = AEConfig.instance.craftItemsByStackAmounts(2);
        final int d = AEConfig.instance.craftItemsByStackAmounts(3);

        this.buttonList.add(this.plus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 26, 22, 20, "+" + a));
        this.buttonList.add(this.plus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 26, 28, 20, "+" + b));
        this.buttonList.add(this.plus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 26, 32, 20, "+" + c));
        this.buttonList.add(this.plus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 26, 38, 20, "+" + d));

        this.buttonList.add(this.minus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 75, 22, 20, "-" + a));
        this.buttonList.add(this.minus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 75, 28, 20, "-" + b));
        this.buttonList.add(this.minus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 75, 32, 20, "-" + c));
        this.buttonList.add(this.minus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 75, 38, 20, "-" + d));

        this.buttonList
                .add(this.set = new GuiButton(0, this.guiLeft + 128, this.guiTop + 51, 38, 20, GuiText.Set.getLocal()));
        this.buttonList.add(
                this.symbolSwitch = new GuiImgButton(
                        this.guiLeft + 22,
                        this.guiTop + 53,
                        Settings.ACTIONS,
                        ActionItems.MULTIPLY));

        ItemStack myIcon = null;
        final Object target = ((AEBaseContainer) this.inventorySlots).getTarget();
        final IDefinitions definitions = AEApi.instance().definitions();
        final IParts parts = definitions.parts();

        if (target instanceof PartPatternTerminal) {
            for (final ItemStack stack : parts.patternTerminal().maybeStack(1).asSet()) {
                myIcon = stack;
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
        }

        else if (target instanceof PartPatternTerminalEx) {
            for (final ItemStack stack : parts.patternTerminalEx().maybeStack(1).asSet()) {
                myIcon = stack;
            }
            this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL_EX;
        }

        if (this.originalGui != null && myIcon != null) {
            this.buttonList.add(
                    this.originalGuiBtn = new GuiTabButton(
                            this.guiLeft + 154,
                            this.guiTop,
                            myIcon,
                            myIcon.getDisplayName(),
                            itemRender));
        }

        this.amountToSet = new GuiTextField(
                this.fontRendererObj,
                this.guiLeft + 50,
                this.guiTop + 57,
                59,
                this.fontRendererObj.FONT_HEIGHT);
        this.amountToSet.setEnableBackgroundDrawing(false);
        this.amountToSet.setMaxStringLength(16);
        this.amountToSet.setTextColor(GuiColors.CraftAmountToCraft.getColor());
        this.amountToSet.setVisible(true);
        this.amountToSet.setFocused(true);
        this.amountToSet.setText(String.valueOf(DEFAULT_VALUE));
        this.amountToSet.setSelectionPos(0);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj
                .drawString(GuiText.SelectAmount.getLocal(), 8, 6, GuiColors.CraftAmountSelectAmount.getColor());
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.set.displayString = GuiText.Set.getLocal();

        this.bindTexture("guis/patternMulti.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

        try {
            String out = this.amountToSet.getText();

            double resultD = Calculator.conversion(out);
            int resultI;

            if (Double.isNaN(resultD)) {
                resultI = DEFAULT_VALUE;
            } else {
                resultI = (int) ArithHelper.round(resultD, 0);
            }
            this.symbolSwitch.set(resultI >= 0 ? ActionItems.MULTIPLY : ActionItems.DIVIDE);
            this.set.enabled = resultI < -1 || resultI > 1;
        } catch (final NumberFormatException e) {
            this.set.enabled = false;
        }

        this.amountToSet.drawTextBox();
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                this.actionPerformed(this.set);
            }
            this.amountToSet.textboxKeyTyped(character, key);
            super.keyTyped(character, key);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {
            if (btn == this.originalGuiBtn) {
                NetworkHandler.instance.sendToServer(new PacketSwitchGuis(this.originalGui));
            }

            if (btn == this.set && btn.enabled) {
                double resultD = Calculator.conversion(this.amountToSet.getText());
                int resultI;

                if (Double.isNaN(resultD)) {
                    resultI = DEFAULT_VALUE;
                } else {
                    resultI = (int) ArithHelper.round(resultD, 0);
                }
                if (resultI > 1 || resultI < -1) NetworkHandler.instance
                        .sendToServer(new PacketPatternMultiSet(this.originalGui.ordinal(), resultI));
            }
        } catch (final NumberFormatException e) {
            // nope..
            this.amountToSet.setText(String.valueOf(DEFAULT_VALUE));
        }

        final boolean isPlus = btn == this.plus1 || btn == this.plus10 || btn == this.plus100 || btn == this.plus1000;
        final boolean isMinus = btn == this.minus1 || btn == this.minus10
                || btn == this.minus100
                || btn == this.minus1000;

        if (isPlus || isMinus) {
            this.addQty(this.getQty(btn));
        } else if (btn == this.symbolSwitch) {

            String out = this.amountToSet.getText();
            double resultD = -Calculator.conversion(out);
            int resultI;
            if (Double.isNaN(resultD)) {
                resultI = DEFAULT_VALUE;
            } else {
                resultI = (int) ArithHelper.round(resultD, 0);
            }
            out = Integer.toString(resultI);
            this.symbolSwitch.set(resultI >= 0 ? ActionItems.MULTIPLY : ActionItems.DIVIDE);
            this.amountToSet.setText(out);
        }

    }

    private void addQty(final int i) {
        try {
            String out = this.amountToSet.getText();

            double resultD = Calculator.conversion(out);
            int resultI;

            if (Double.isNaN(resultD)) {
                resultI = DEFAULT_VALUE;
            } else {
                resultI = (int) ArithHelper.round(resultD, 0);
            }

            resultI += i;

            out = Integer.toString(resultI);
            this.symbolSwitch.set(resultI >= 0 ? ActionItems.MULTIPLY : ActionItems.DIVIDE);
            this.amountToSet.setText(out);
        } catch (final NumberFormatException e) {
            // :P
        }
    }

    protected String getBackground() {
        return "guis/patternMulti.png";
    }
}

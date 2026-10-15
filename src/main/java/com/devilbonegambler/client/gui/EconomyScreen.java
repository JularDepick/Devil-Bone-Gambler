/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.block.EconomyBlock;
import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.config.ShopConfig;
import com.devilbonegambler.config.ShopOffer;
import com.devilbonegambler.economy.CurrencyType;
import com.devilbonegambler.economy.EconomyOperation;
import com.devilbonegambler.economy.EconomyService;
import com.devilbonegambler.util.ModConstants;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * One screen for the four economy blocks, switching its body on the block kind.
 * Every action only sends an intent; the server re-validates balances, inventory and config legality.
 */
public class EconomyScreen extends Screen
{
    private static final Gson GSON = new Gson();

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 220;
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int ROW_COLOR = 0x20FFFFFF;
    private static final int ROW_SELECTED_COLOR = 0x60FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int STATUS_COLOR = 0x80C0FF;
    private static final int ROW_HEIGHT = 11;
    private static final int VISIBLE_ROWS = 8;
    private static final int LIST_X = 8;
    private static final int LIST_WIDTH = 304;
    private static final int LIST_Y = 54;

    private final EconomyBlock.Kind kind;
    private final List<String> rows = new ArrayList<String>();
    private final List<String> rowIds = new ArrayList<String>();
    private final List<ShopOffer> shopOffers = new ArrayList<ShopOffer>();
    private final Map<String, Integer> recycleSelection = new LinkedHashMap<String, Integer>();

    private int leftPos;
    private int topPos;
    private int scroll;
    private int selected = -1;
    private String status = "";
    private RatioSlider ratioSlider;

    private EditBox amountBox;
    private EditBox priceBox;
    private EditBox targetBox;
    private Button primaryButton;
    private Button secondaryButton;

    public EconomyScreen(EconomyBlock.Kind kind)
    {
        super(Component.translatable("gui." + ModConstants.MOD_ID + ".economy." + kind.name().toLowerCase()));
        this.kind = kind;
    }

    @Override
    protected void init()
    {
        super.init();
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        int fieldY = this.topPos + PANEL_HEIGHT - 52;
        this.amountBox = new EditBox(this.font, this.leftPos + 8, fieldY, 60, 14,
                Component.translatable(key("economy.amount")));
        this.amountBox.setFilter(EconomyScreen::isDigits);
        this.amountBox.setValue("1");
        addRenderableWidget(this.amountBox);

        this.priceBox = new EditBox(this.font, this.leftPos + 74, fieldY, 60, 14,
                Component.translatable(key("economy.price")));
        this.priceBox.setFilter(EconomyScreen::isDigits);
        this.priceBox.setValue("1");
        addRenderableWidget(this.priceBox);

        this.targetBox = new EditBox(this.font, this.leftPos + 140, fieldY, 110, 14,
                Component.translatable(key("economy.target")));
        addRenderableWidget(this.targetBox);

        int buttonY = this.topPos + PANEL_HEIGHT - 34;
        this.primaryButton = addRenderableWidget(
                Button.builder(Component.translatable(key("economy.primary." + this.kind.name().toLowerCase())),
                        button -> primary()).pos(this.leftPos + 8, buttonY).size(96, 18).build());
        this.secondaryButton = addRenderableWidget(
                Button.builder(Component.translatable(key("economy.secondary." + this.kind.name().toLowerCase())),
                        button -> secondary()).pos(this.leftPos + 110, buttonY).size(96, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("economy.close")), button -> onClose())
                .pos(this.leftPos + 212, buttonY).size(100, 18).build());

        this.amountBox.setVisible(this.kind == EconomyBlock.Kind.BANK || this.kind == EconomyBlock.Kind.RECYCLER);
        this.priceBox.setVisible(this.kind == EconomyBlock.Kind.EXCHANGE);
        this.targetBox.setVisible(this.kind == EconomyBlock.Kind.BANK || this.kind == EconomyBlock.Kind.EXCHANGE);

        if (this.kind == EconomyBlock.Kind.SHOP)
        {
            /*
             * The slider sets how much of the price is paid with the item form, the remainder comes
             * out of the account balance. The two currencies never substitute for each other.
             */
            this.ratioSlider = new RatioSlider(this.leftPos + 8, this.topPos + PANEL_HEIGHT - 68, 160, 14, 0.0D);
            addRenderableWidget(this.ratioSlider);
        }

        refreshRows();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 8, TITLE_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("economy.balance"),
                EconomyService.format(ClientActions.economyCoin()), EconomyService.format(ClientActions.economyTear())),
                this.leftPos + 8, this.topPos + 20, TEXT_COLOR, false);

        if (this.kind == EconomyBlock.Kind.EXCHANGE)
        {
            graphics.drawString(this.font, Component.translatable(key("economy.vip"),
                    ClientActions.economyVipActive() ? Component.translatable(key("economy.vip_on"))
                            : Component.translatable(key("economy.vip_off")),
                    Integer.valueOf(ClientActions.economyPending())),
                    this.leftPos + 8, this.topPos + 32, LABEL_COLOR, false);
        }
        else if (this.kind == EconomyBlock.Kind.BANK)
        {
            graphics.drawString(this.font, Component.translatable(key("economy.counter"),
                    EconomyService.format(ClientActions.economyCounterCoin()),
                    EconomyService.format(ClientActions.economyCounterTear())),
                    this.leftPos + 8, this.topPos + 32, LABEL_COLOR, false);
        }
        else if (this.kind == EconomyBlock.Kind.SHOP)
        {
            drawPaymentPreview(graphics);
        }

        drawList(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.status, this.leftPos + 8, this.topPos + PANEL_HEIGHT - 46, STATUS_COLOR,
                false);
        graphics.drawString(this.font, Component.translatable(key("economy.hint." + this.kind.name().toLowerCase())),
                this.leftPos + 8, this.topPos + 42, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"),
                this.leftPos + 8, this.topPos + PANEL_HEIGHT - 12, LABEL_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int index = rowAt(mouseX, mouseY);

        if (index >= 0)
        {
            onRowClicked(index);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        int maximum = Math.max(0, this.rows.size() - VISIBLE_ROWS);

        if (delta > 0.0D)
        {
            this.scroll = Math.max(0, this.scroll - 1);
        }
        else if (delta < 0.0D)
        {
            this.scroll = Math.min(maximum, this.scroll + 1);
        }

        return true;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    /**
     * Called by the client packet handler when the server pushes a fresh snapshot.
     */
    public void onDataUpdated()
    {
        refreshRows();
    }

    private void refreshRows()
    {
        this.rows.clear();
        this.rowIds.clear();
        this.shopOffers.clear();

        if (this.kind == EconomyBlock.Kind.SHOP)
        {
            ShopConfig shop = parseShop(ClientActions.economyShopJson());

            for (ShopOffer offer : shop.offersOrEmpty())
            {
                if (!offer.isEnabled())
                {
                    continue;
                }

                this.shopOffers.add(offer);
                this.rowIds.add(offer.id);
                this.rows.add(Component.translatable(key("economy.shop_row"), offer.itemOrEmpty(),
                        Integer.valueOf(offer.countOrZero()), EconomyService.format(offer.priceCoinOrZero()),
                        EconomyService.format(offer.priceTearOrZero())).getString());
            }
        }
        else if (this.kind == EconomyBlock.Kind.RECYCLER)
        {
            Inventory inventory = this.minecraft == null || this.minecraft.player == null
                    ? null
                    : this.minecraft.player.getInventory();

            if (inventory != null)
            {
                for (int slot = 0; slot < inventory.items.size(); slot++)
                {
                    ItemStack stack = inventory.items.get(slot);

                    if (stack.isEmpty())
                    {
                        continue;
                    }

                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());

                    if (id == null)
                    {
                        continue;
                    }

                    this.rowIds.add(id.toString());
                    this.rows.add(Component.translatable(key("economy.recycle_row"), id.toString(),
                            Integer.valueOf(stack.getCount())).getString());
                }
            }
        }
    }

    /**
     * Payment preview for the selected offer: how much of each currency comes out of the item form
     * and how much out of the balance, with the current balances next to it.
     */
    private void drawPaymentPreview(GuiGraphics graphics)
    {
        if (this.selected < 0 || this.selected >= this.shopOffers.size())
        {
            return;
        }

        ShopOffer offer = this.shopOffers.get(this.selected);
        double ratio = this.ratioSlider == null ? 0.0D : this.ratioSlider.ratio();
        long itemCoin = (long) Math.floor(offer.priceCoinOrZero() * ratio);
        long itemTear = (long) Math.floor(offer.priceTearOrZero() * ratio);
        long balanceCoin = Math.max(0L, offer.priceCoinOrZero() - itemCoin);
        long balanceTear = Math.max(0L, offer.priceTearOrZero() - itemTear);
        boolean affordable = ClientActions.economyCoin() >= balanceCoin && ClientActions.economyTear() >= balanceTear;

        graphics.drawString(this.font, Component.translatable(key("economy.pay_preview"),
                EconomyService.format(itemCoin), EconomyService.format(balanceCoin),
                EconomyService.format(itemTear), EconomyService.format(balanceTear)),
                this.leftPos + 8, this.topPos + 32, affordable ? LABEL_COLOR : STATUS_COLOR, false);
    }

    private void drawList(GuiGraphics graphics)
    {
        for (int line = 0; line < VISIBLE_ROWS; line++)
        {
            int index = this.scroll + line;

            if (index >= this.rows.size())
            {
                break;
            }

            int y = this.topPos + LIST_Y + line * ROW_HEIGHT;
            graphics.fill(this.leftPos + LIST_X, y, this.leftPos + LIST_X + LIST_WIDTH, y + ROW_HEIGHT - 1,
                    index == this.selected ? ROW_SELECTED_COLOR : ROW_COLOR);
            graphics.drawString(this.font, this.rows.get(index), this.leftPos + LIST_X + 2, y + 2, TEXT_COLOR, false);
        }
    }

    private int rowAt(double mouseX, double mouseY)
    {
        if (mouseX < this.leftPos + LIST_X || mouseX > this.leftPos + LIST_X + LIST_WIDTH)
        {
            return -1;
        }

        int line = (int) Math.floor((mouseY - (this.topPos + LIST_Y)) / ROW_HEIGHT);

        if (line < 0 || line >= VISIBLE_ROWS)
        {
            return -1;
        }

        int index = this.scroll + line;
        return index < this.rows.size() ? index : -1;
    }

    private void onRowClicked(int index)
    {
        this.selected = index;

        if (this.kind == EconomyBlock.Kind.RECYCLER && index < this.rowIds.size())
        {
            String id = this.rowIds.get(index);
            Integer current = this.recycleSelection.get(id);
            this.recycleSelection.put(id, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
            this.status = Component.translatable(key("economy.selected"), id,
                    Integer.valueOf(this.recycleSelection.get(id).intValue())).getString();
        }
    }

    private void primary()
    {
        long amount = parseLong(this.amountBox.getValue(), 0L);

        switch (this.kind)
        {
            case BANK:
                send(EconomyOperation.BANK_DEPOSIT, CurrencyType.COIN, amount, 0L, 0, "", "");
                break;
            case SHOP:
                if (this.selected >= 0 && this.selected < this.shopOffers.size())
                {
                    ShopOffer offer = this.shopOffers.get(this.selected);
                    double ratio = this.ratioSlider == null ? 0.0D : this.ratioSlider.ratio();
                    long itemShareCoin = (long) Math.floor(offer.priceCoinOrZero() * ratio);
                    long itemShareTear = (long) Math.floor(offer.priceTearOrZero() * ratio);
                    send(EconomyOperation.SHOP_BUY, CurrencyType.COIN, itemShareCoin, itemShareTear, 0,
                            offer.id, "");
                }
                else
                {
                    this.status = Component.translatable(key("economy.need_selection")).getString();
                }
                break;
            case EXCHANGE:
                send(EconomyOperation.EXCHANGE_PUBLISH_SELL, CurrencyType.COIN, parseLong(this.priceBox.getValue(), 0L),
                        0L, 0, "", "");
                break;
            case RECYCLER:
                recycle();
                break;
            default:
                break;
        }
    }

    private void secondary()
    {
        long amount = parseLong(this.amountBox.getValue(), 0L);

        switch (this.kind)
        {
            case BANK:
                if (this.targetBox.getValue().isEmpty())
                {
                    send(EconomyOperation.BANK_WITHDRAW, CurrencyType.COIN, amount, 0L, 0, "", "");
                }
                else
                {
                    send(EconomyOperation.BANK_TRANSFER, CurrencyType.COIN, amount, 0L, 0, this.targetBox.getValue(), "");
                }

                break;
            case SHOP:
                send(EconomyOperation.BANK_WITHDRAW, CurrencyType.TEAR, amount, 0L, 0, "", "");
                break;
            case EXCHANGE:
                if (this.targetBox.getValue().isEmpty())
                {
                    send(EconomyOperation.EXCHANGE_COLLECT, CurrencyType.COIN, 0L, 0L, 0, "", "");
                }
                else
                {
                    send(EconomyOperation.EXCHANGE_ACCEPT, CurrencyType.COIN, 0L, 0L, 0, this.targetBox.getValue(), "");
                }

                break;
            case RECYCLER:
                this.recycleSelection.clear();
                this.status = Component.translatable(key("economy.cleared")).getString();
                break;
            default:
                break;
        }
    }

    private void recycle()
    {
        if (this.recycleSelection.isEmpty())
        {
            this.status = Component.translatable(key("economy.need_selection")).getString();
            return;
        }

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : this.recycleSelection.entrySet())
        {
            if (builder.length() > 0)
            {
                builder.append(';');
            }

            builder.append(entry.getKey()).append(':').append(entry.getValue().intValue());
        }

        send(EconomyOperation.RECYCLE, CurrencyType.COIN, 0L, 0L, 0, builder.toString(), "");
        this.recycleSelection.clear();
    }

    private void send(EconomyOperation operation, CurrencyType currency, long amount, long amountB, int count,
            String textA, String textB)
    {
        ClientActions.sendEconomyRequest(operation, currency, amount, amountB, count, textA, textB);
        this.status = Component.translatable(key("economy.sent")).getString();
    }

    private static ShopConfig parseShop(String json)
    {
        try
        {
            ShopConfig parsed = GSON.fromJson(json, ShopConfig.class);
            return parsed == null ? ShopConfig.empty() : parsed;
        }
        catch (RuntimeException exception)
        {
            return ShopConfig.empty();
        }
    }

    /**
     * Digit only filter for the amount and price fields.
     */
    private static boolean isDigits(String value)
    {
        if (value == null || value.isEmpty())
        {
            return true;
        }

        for (int index = 0; index < value.length(); index++)
        {
            if (!Character.isDigit(value.charAt(index)))
            {
                return false;
            }
        }

        return true;
    }

    private static long parseLong(String raw, long fallback)    {
        try
        {
            return Long.parseLong(raw.trim());
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static String key(String suffix)
    {
        return "gui." + ModConstants.MOD_ID + "." + suffix;
    }

    /**
     * Ratio control for the item form share of the payment.
     * The slider value field is protected, so it is only reachable from a subclass.
     */
    private static final class RatioSlider extends net.minecraft.client.gui.components.AbstractSliderButton
    {
        private RatioSlider(int x, int y, int width, int height, double value)
        {
            super(x, y, width, height, Component.empty(), value);
        }

        private double ratio()
        {
            return this.value;
        }

        @Override
        protected void updateMessage()
        {
            setMessage(Component.translatable(key("economy.ratio"),
                    Integer.valueOf((int) Math.round(this.value * 100.0D))));
        }

        @Override
        protected void applyValue()
        {
        }
    }
}

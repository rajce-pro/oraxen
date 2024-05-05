package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class ItemsView {

    private final YamlConfiguration settings = OraxenPlugin.get().resourceManager().getSettings();

    BaseGui mainGui;

    public BaseGui create() {
        final Map<File, PaginatedGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (!unexcludedItems.isEmpty())
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        int rows = (int) Settings.ORAXEN_INV_ROWS.getValue();
        String invType = Settings.ORAXEN_INV_TYPE.toString();
        mainGui = (Objects.equals(invType, "PAGINATED")
                ? Gui.paginated().pageSize((rows - 1) * 9)
                : (Objects.equals(invType, "SCROLL_HORIZONTAL")
                ? Gui.scrolling(ScrollType.HORIZONTAL).pageSize((rows - 1) * 9)
                : (Objects.equals(invType, "SCROLL_VERTICAL")
                ? Gui.scrolling(ScrollType.VERTICAL).pageSize((rows - 1) * 9)
                : Gui.gui()))).rows(rows).title(Settings.ORAXEN_INV_TITLE.toComponent()).create();

        List<Integer> usedSlots = files.keySet().stream().map(this::getItemStack).map(Pair::getValue).sorted().toList();
        int highestUsedSlot = usedSlots.get(usedSlots.size() - 1);
        GuiItem emptyGuiItem = new GuiItem(Material.AIR);
        List<GuiItem> guiItems = new ArrayList<>(Collections.nCopies(highestUsedSlot + 1, emptyGuiItem));

        for (Map.Entry<File, PaginatedGui> entry : files.entrySet()) {
            int slot = getItemStack(entry.getKey()).getRight();
            if (slot == -1) continue;
            guiItems.set(slot, new GuiItem(getItemStack(entry.getKey()).getLeft(), e -> entry.getValue().open(e.getWhoClicked())));
        }

        // Add all items without a specified slot to the earliest available slot
        for (Map.Entry<File, PaginatedGui> entry : files.entrySet()) {
            int slot = getItemStack(entry.getKey()).getRight();
            if (slot != -1) continue;
            guiItems.set(guiItems.indexOf(emptyGuiItem), new GuiItem(getItemStack(entry.getKey()).getLeft(), e -> entry.getValue().open(e.getWhoClicked())));
        }

        mainGui.addItem(true, guiItems.toArray(new GuiItem[]{}));

        ItemStack nextPage = (Settings.ORAXEN_INV_NEXT_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById(Settings.ORAXEN_INV_NEXT_ICON.toString()))
                .displayName(Component.text("Next Page")).build();
        ItemStack previousPage = (Settings.ORAXEN_INV_PREVIOUS_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById(Settings.ORAXEN_INV_PREVIOUS_ICON.toString()))
                .displayName(Component.text("Previous Page")).build();
        ItemStack exitIcon = (Settings.ORAXEN_INV_EXIT.getValue() == null
                ? new ItemBuilder(Material.BARRIER) : OraxenItems.getItemById(Settings.ORAXEN_INV_EXIT.toString()))
                .displayName(Component.text("Exit")).build();

        if (mainGui instanceof PaginatedGui paginated) {
            if (paginated.getPagesNum() > 1) {
                paginated.setItem(6, 2, new GuiItem(previousPage, event -> {
                    paginated.previous();
                    event.setCancelled(true);
                }));
                paginated.setItem(6, 8, new GuiItem(nextPage, event -> {
                    paginated.next();
                    event.setCancelled(true);
                }));
            }

            paginated.setItem(6, 5, new GuiItem(exitIcon, event -> mainGui.open(event.getWhoClicked())));
        }

        return mainGui;
    }

    private PaginatedGui createSubGUI(final String fileName, final List<ItemBuilder> items) {
        final PaginatedGui gui = Gui.paginated().rows(6).pageSize(45).title(AdventureUtils.MINI_MESSAGE.deserialize(settings.getString(
                        String.format("oraxen_inventory.menu_layout.%s.title", Utils.removeExtension(fileName)), Settings.ORAXEN_INV_TITLE.toString())
                .replace("<main_menu_title>", Settings.ORAXEN_INV_TITLE.toString()))).create();
        gui.disableAllInteractions();

        for (ItemBuilder builder : items) {
            if (builder == null) continue;
            ItemStack itemStack = builder.build();
            if (ItemUtils.isEmpty(itemStack)) continue;

            gui.addItem(new GuiItem(itemStack, e -> e.getWhoClicked().getInventory().addItem(ItemUpdater.updateItem(e.getCurrentItem().clone()))));
        }

        ItemStack nextPage = (Settings.ORAXEN_INV_NEXT_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById(Settings.ORAXEN_INV_NEXT_ICON.toString()))
                .displayName(Component.text("Next Page")).build();
        ItemStack previousPage = (Settings.ORAXEN_INV_PREVIOUS_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById(Settings.ORAXEN_INV_PREVIOUS_ICON.toString()))
                .displayName(Component.text("Previous Page")).build();
        ItemStack exitIcon = (Settings.ORAXEN_INV_EXIT.getValue() == null
                ? new ItemBuilder(Material.BARRIER) : OraxenItems.getItemById(Settings.ORAXEN_INV_EXIT.toString()))
                .displayName(Component.text("Exit")).build();

        if (gui.getPagesNum() > 1) {
            gui.setItem(6, 2, new GuiItem(previousPage, event -> gui.previous()));
            gui.setItem(6, 8, new GuiItem(nextPage, event -> gui.next()));
        }

        gui.setItem(6, 5, new GuiItem(exitIcon, event -> mainGui.open(event.getWhoClicked())));

        return gui;
    }

    private Pair<ItemStack, Integer> getItemStack(final File file) {
        ItemStack itemStack;
        String fileName = Utils.removeExtension(file.getName());
        String material = settings.getString(String.format("oraxen_inventory.menu_layout.%s.icon", fileName), "PAPER");
        Component displayName = ItemParser.parseComponentItemName(settings.getString(String.format("oraxen_inventory.menu_layout.%s.displayname", fileName), "<green>" + file.getName()));
        try {
            itemStack = new ItemBuilder(OraxenItems.getItemById(material).getReferenceClone())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .displayName(displayName)
                    .setLore(new ArrayList<>())
                    .build();
        } catch (final Exception e) {
            try {
                itemStack = new ItemBuilder(Material.getMaterial(material.toUpperCase()))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .displayName(displayName)
                        .build();
            } catch (final Exception ignored) {
                itemStack = new ItemBuilder(Material.PAPER)
                        .displayName(displayName)
                        .build();
            }
        }

        // avoid possible bug if isOraxenItems is available but can't be an itemstack
        if (itemStack == null) itemStack = new ItemBuilder(Material.PAPER).displayName(displayName).build();
        int slot = settings.getInt(String.format("oraxen_inventory.menu_layout.%s.slot", Utils.removeExtension(file.getName())), -1) - 1;
        return Pair.of(itemStack, Math.max(slot, -1));
    }
}

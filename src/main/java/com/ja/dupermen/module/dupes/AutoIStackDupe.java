package com.ja.dupermen.module.dupes;

import com.ja.dupermen.module.Category;
import com.ja.dupermen.module.Module;
import com.ja.dupermen.setting.Setting;
import com.ja.dupermen.util.InventoryUtil;
import com.ja.dupermen.util.MessageUtils;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;

import java.util.Comparator;

public class AutoIStackDupe extends Module {

    public AutoIStackDupe() {
        super(
                "IllegalStackDupe",
                "idfk",
                Category.DUPES
        );
        addSetting(onGroundCheck);
    }

    Setting onGroundCheck = new Setting("OnGround Check", true);

    boolean mounted, interacted, opened, moved,chestRemoved, thrown, dismounted, screenClosed;
    int i = 0;

    public void reset() {
        mounted = false;
        interacted = false;
        opened = false;
        moved = false;
        chestRemoved = false;
        thrown = false;
        dismounted = false;
        screenClosed = false;
        i = 0;
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onUpdate() {
        if (fullNullCheck()) return;
        i++;
        if (onGroundCheck.getBVal()) {
            if (i == 1 && !mc.player.onGround) return;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) toggle();
        if (mounted && interacted && opened && moved && chestRemoved && thrown && dismounted && screenClosed || i == 8) {
            reset();
        }
        if (i == 0) {
            if (InventoryUtil.getItemHotbar(Item.getItemFromBlock(Blocks.CHEST)) == -1) {
                MessageUtils.sendSuffixMessage(mc.player, "no chests");
                toggle();
            } else {
                InventoryUtil.switchToHotbarSlot(Blocks.CHEST, false);
                MessageUtils.sendSuffixMessage(mc.player, "switched");
            }
        }
        Entity donkey = mc.world.loadedEntityList.stream().filter(this::isGood).min(Comparator.comparing(e->mc.player.getDistance(e))).orElse(null);
        if (!mounted && i == 1) {
            if (donkey != null) {
                mc.playerController.interactWithEntity(mc.player, donkey, EnumHand.MAIN_HAND);
                mc.playerController.interactWithEntity(mc.player, donkey, EnumHand.MAIN_HAND);
                MessageUtils.sendSuffixMessage(mc.player, "interacted");
                interacted = true;
                mc.player.sendHorseInventory();
                MessageUtils.sendSuffixMessage(mc.player, "opened");
                opened = true;
            } else {
                MessageUtils.sendSuffixMessage(mc.player, "no valid entity");
                toggle();
            }
            mounted = true;
        }
        if (mc.currentScreen instanceof GuiScreenHorseInventory && opened && i == 2) {
//            int b = 0;
//            while (b != 15) {
                for (int i = 0; i < 32; i++) {
                    if (mc.player.openContainer.getSlot(i).getStack().getItem() instanceof ItemShulkerBox) {
                        mc.playerController.windowClick(mc.player.openContainer.windowId, i, 0, ClickType.QUICK_MOVE, mc.player);
//                        b++;
                    }
                }
//            }
            MessageUtils.sendSuffixMessage(mc.player, "moved");
            moved = true;
        }
        if (moved && donkey != null && i == 3) {
            mc.player.connection.sendPacket(new CPacketUseEntity(donkey, EnumHand.MAIN_HAND, new Vec3d(0f,0f,0f)));
            mc.gameSettings.keyBindSneak.pressed = true;
            MessageUtils.sendSuffixMessage(mc.player, "removed chest");
            chestRemoved = true;
        }
        if (chestRemoved && i == 4) {
            for (int i = 2; i<17; i++) {
                mc.playerController.windowClick(mc.player.openContainer.windowId, i, 0, ClickType.THROW, mc.player);
            }
            MessageUtils.sendSuffixMessage(mc.player, "throwing stuff");
            thrown = true;
        }
        if (thrown && i == 5) {
            mc.player.closeScreen();
            MessageUtils.sendSuffixMessage(mc.player, "dismount");
            mc.gameSettings.keyBindSneak.pressed = true;
            screenClosed = true;
        }
        if (screenClosed && i == 6) {
            mc.player.closeScreen();
            MessageUtils.sendSuffixMessage(mc.player, "re-enabling");
            mc.gameSettings.keyBindSneak.pressed = false;
        }
    }

    public boolean isGood(Entity entity) {
        return entity instanceof EntityLlama || entity instanceof EntityDonkey || entity instanceof EntityMule;
    }
}

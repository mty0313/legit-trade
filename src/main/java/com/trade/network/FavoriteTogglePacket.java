package com.trade.network;

import com.trade.LegitTrade;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class FavoriteTogglePacket {
    public static final Identifier ID = new Identifier(LegitTrade.MOD_ID, "favorite_toggle");

    private static final int MAX_GROUP_NAME_LENGTH = 64;
    private static final int MAX_TRADE_KEY_LENGTH = 512;

    private FavoriteTogglePacket() {
    }

    public static PacketByteBuf write(String groupName, String tradeKey, boolean favorite) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(groupName, MAX_GROUP_NAME_LENGTH);
        buf.writeString(tradeKey, MAX_TRADE_KEY_LENGTH);
        buf.writeBoolean(favorite);
        return buf;
    }

    public static Payload read(PacketByteBuf buf) {
        String groupName = buf.readString(MAX_GROUP_NAME_LENGTH);
        String tradeKey = buf.readString(MAX_TRADE_KEY_LENGTH);
        boolean favorite = buf.readBoolean();
        return new Payload(groupName, tradeKey, favorite);
    }

    public record Payload(String groupName, String tradeKey, boolean favorite) {
    }
}
